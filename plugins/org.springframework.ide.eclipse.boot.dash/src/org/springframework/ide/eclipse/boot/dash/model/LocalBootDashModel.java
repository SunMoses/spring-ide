/*******************************************************************************
 * Copyright (c) 2015, 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.boot.dash.model;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.IJavaProject;
import org.springframework.ide.eclipse.boot.core.cli.BootCliCommand;
import org.springframework.ide.eclipse.boot.core.cli.BootCliUtils;
import org.springframework.ide.eclipse.boot.core.cli.BootInstallManager;
import org.springframework.ide.eclipse.boot.core.cli.BootInstallManager.BootInstallListener;
import org.springframework.ide.eclipse.boot.core.cli.install.IBootInstall;
import org.springframework.ide.eclipse.boot.dash.devtools.DevtoolsPortRefresher;
import org.springframework.ide.eclipse.boot.dash.util.LaunchConfRunStateTracker;
import org.springframework.ide.eclipse.boot.dash.util.LaunchConfigurationTracker;
import org.springframework.ide.eclipse.boot.dash.views.BootDashModelConsoleManager;
import org.springframework.ide.eclipse.boot.dash.views.BootDashTreeView;
import org.springframework.ide.eclipse.boot.dash.views.LocalElementConsoleManager;
import org.springframework.ide.eclipse.boot.launch.BootLaunchConfigurationDelegate;
import org.springframework.ide.eclipse.boot.util.Log;
import org.springsource.ide.eclipse.commons.frameworks.core.workspace.ClasspathListenerManager;
import org.springsource.ide.eclipse.commons.frameworks.core.workspace.ClasspathListenerManager.ClasspathListener;
import org.springsource.ide.eclipse.commons.frameworks.core.workspace.ProjectChangeListenerManager;
import org.springsource.ide.eclipse.commons.frameworks.core.workspace.ProjectChangeListenerManager.ProjectChangeListener;
import org.springsource.ide.eclipse.commons.livexp.core.AsyncLiveExpression.AsyncMode;
import org.springsource.ide.eclipse.commons.livexp.core.LiveExpression;
import org.springsource.ide.eclipse.commons.livexp.core.LiveSetVariable;
import org.springsource.ide.eclipse.commons.livexp.core.ObservableSet;
import org.springsource.ide.eclipse.commons.livexp.core.ValueListener;

/**
 * Model of the contents for {@link BootDashTreeView}, provides mechanism to attach listeners to model
 * and attaches itself as a workspace listener to keep the model in synch with workspace changes.
 *
 * @author Kris De Volder
 */
public class LocalBootDashModel extends AbstractBootDashModel implements DeletionCapabableModel {

	private IWorkspace workspace;
	private BootProjectDashElementFactory projectElementFactory;
	private LaunchConfDashElementFactory launchConfElementFactory;

	ProjectChangeListenerManager openCloseListenerManager;
	ClasspathListenerManager classpathListenerManager;

	private final LaunchConfRunStateTracker launchConfRunStateTracker = new LaunchConfRunStateTracker();
	private final LocalServiceRunStateTracker launchConfLocalServiceRunStateTracker = new LocalServiceRunStateTracker();
	final LaunchConfigurationTracker launchConfTracker = new LaunchConfigurationTracker(BootLaunchConfigurationDelegate.TYPE_ID);

	LiveSetVariable<BootDashElement> elements; //lazy created
	private BootDashModelConsoleManager consoleManager;

	private DevtoolsPortRefresher devtoolsPortRefresher;
	private LiveExpression<Pattern> projectExclusion;
	private ValueListener<Pattern> projectExclusionListener;

	private BootInstallListener bootInstallListener;

	public class WorkspaceListener implements ProjectChangeListener, ClasspathListener {

		@Override
		public void projectChanged(IProject project) {
			updateElementsFromWorkspace();
		}

		@Override
		public void classpathChanged(IJavaProject jp) {
			updateElementsFromWorkspace();
		}
	}

	public LocalBootDashModel(BootDashModelContext context, BootDashViewModel parent) {
		super(RunTargets.LOCAL, parent);
		this.workspace = context.getWorkspace();
		this.launchConfElementFactory = new LaunchConfDashElementFactory(this, context.getLaunchManager());
		this.projectElementFactory = new BootProjectDashElementFactory(this, context.getProjectProperties(), launchConfElementFactory);
		this.consoleManager = new LocalElementConsoleManager();
		this.projectExclusion = context.getBootProjectExclusion();
	}

	void init() {
		if (elements==null) {
			this.elements = new LiveSetVariable<>(AsyncMode.SYNC);
			WorkspaceListener workspaceListener = new WorkspaceListener();
			this.openCloseListenerManager = new ProjectChangeListenerManager(workspace, workspaceListener);
			this.classpathListenerManager = new ClasspathListenerManager(workspaceListener);
			projectExclusion.addListener(projectExclusionListener = new ValueListener<Pattern>() {
				public void gotValue(LiveExpression<Pattern> exp, Pattern value) {
					updateElementsFromWorkspace();
				}
			});
			updateElementsFromWorkspace();

			bootInstallListener = new BootInstallListener() {
				@Override
				public void defaultInstallChanged() {
					refresh(null);
				}
			};
			try {
				BootInstallManager.getInstance().addBootInstallListener(bootInstallListener);
			} catch (Exception e) {
				Log.log(e);
			}
			new Job("Loading local cloud services") {
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					fetchLocalServices();
					return Status.OK_STATUS;
				}
			}.schedule();

			this.devtoolsPortRefresher = new DevtoolsPortRefresher(this, projectElementFactory);
		}
	}

	/**
	 * When no longer needed the model should be disposed, otherwise it will continue
	 * listening for changes to the workspace in order to keep itself in synch.
	 */
	public void dispose() {
		if (elements!=null) {
			elements = null;
			openCloseListenerManager.dispose();
			openCloseListenerManager = null;
			classpathListenerManager.dispose();
			classpathListenerManager = null;
			devtoolsPortRefresher.dispose();
			devtoolsPortRefresher = null;
		}
		if (launchConfElementFactory!=null) {
			launchConfElementFactory.dispose();
			launchConfElementFactory = null;
		}
		if (projectElementFactory!=null) {
			projectElementFactory.dispose();
			projectElementFactory = null;
		}
		if (projectExclusionListener!=null) {
			projectExclusion.removeListener(projectExclusionListener);
			projectExclusionListener=null;
		}
		if (bootInstallListener != null) {
			try {
				BootInstallManager.getInstance().removeBootInstallListener(bootInstallListener);
			} catch (Exception e) {
				Log.log(e);
			}
		}
		launchConfTracker.dispose();
		launchConfRunStateTracker.dispose();
		launchConfLocalServiceRunStateTracker.dispose();
	}

	void updateElementsFromWorkspace() {
		Set<BootDashElement> newElements = new HashSet<>();
		for (IProject p : this.workspace.getRoot().getProjects()) {
			BootDashElement element = projectElementFactory.createOrGet(p);
			if (element!=null) {
				newElements.add(element);
			}
		}
		elements.replaceAll(newElements);
		projectElementFactory.disposeAllExcept(newElements);
	}

	public synchronized ObservableSet<BootDashElement> getElements() {
		init();
		return elements;
	}

	/**
	 * Trigger manual model refresh.
	 */
	public void refresh(UserInteractions ui) {
		updateElementsFromWorkspace();
		new Job("Loading local cloud services") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				fetchLocalServices();
				return Status.OK_STATUS;
			}
		}.schedule();
	}

	private void fetchLocalServices() {
		try {
			IBootInstall bootInstall = BootCliUtils.getSpringBootInstall();
			if (BootCliUtils.supportsSpringCloud(bootInstall)) {
				BootCliCommand cmd = new BootCliCommand(BootCliUtils.getSpringBootHome(bootInstall));
				try {
					cmd.execute("cloud", "--list");
					List<BootDashElement> localServices = new LinkedList<>();
					if (!cmd.getOutput().startsWith("Exception in thread")) {
						String[] outputLines = cmd.getOutput().split("\n");
						String servicesLine = outputLines[outputLines.length - 1];
						for (String id : servicesLine.split(" ")) {
							localServices.add(new LocalCloudServiceDashElement(this, id));
						}
					}
					elements.addAll(localServices);
				} catch (RuntimeException e) {
					Log.log(e);
				}
			}
		} catch (Exception e) {
			// ignore
		}
	}

	@Override
	public BootDashModelConsoleManager getElementConsoleManager() {
		return consoleManager;
	}

	public LaunchConfRunStateTracker getLaunchConfRunStateTracker() {
		return launchConfRunStateTracker;
	}

	public LocalServiceRunStateTracker getLaunchConfLocalServiceRunStateTracker() {
		return launchConfLocalServiceRunStateTracker;
	}

	public BootProjectDashElementFactory getProjectElementFactory() {
		return projectElementFactory;
	}

	public LaunchConfDashElementFactory getLaunchConfElementFactory() {
		return launchConfElementFactory;
	}

	@Override
	public void delete(Collection<BootDashElement> elements, UserInteractions ui) {
		for (BootDashElement e : elements) {
			if (e instanceof Deletable) {
				((Deletable)e).delete(ui);
			}
		}
	}

	@Override
	public boolean canDelete(BootDashElement element) {
		return element instanceof Deletable;
	}

	@Override
	public String getDeletionConfirmationMessage(Collection<BootDashElement> value) {
		return "Are you sure you want to delete the selected local launch configuration(s)? The configuration(s) will be permanently removed from the workspace.";
	}
}
