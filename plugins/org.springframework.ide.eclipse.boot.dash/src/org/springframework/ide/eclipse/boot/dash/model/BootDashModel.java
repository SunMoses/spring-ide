package org.springframework.ide.eclipse.boot.dash.model;

import java.util.Comparator;

import org.springframework.ide.eclipse.boot.dash.livexp.ObservableSet;
import org.springframework.ide.eclipse.boot.dash.views.BootDashModelConsoleManager;

public interface BootDashModel {

	interface ModelStateListener {
		void stateChanged(BootDashModel model);
	}

	interface ElementStateListener {
		/**
		 * Called when something about the element has changed.
		 * <p>
		 * Note this doesn't get called when (top-level) elements are
		 * added / removed to the model. Only when some property of
		 * the element itself has changed.
		 * <p>
		 * Note: think of the 'children' of an element as a propery of its parent element.
		 * So, if a child is added/removed to/from an element then the element
		 * itself will receive a stateChanged event.
		 */
		void stateChanged(BootDashElement e);
	}

	RunTarget getRunTarget();

	ObservableSet<BootDashElement> getElements();

	BootDashModelConsoleManager getElementConsoleManager();

	/**
	 * When no longer needed the model should be disposed, otherwise it will
	 * continue listening for changes to the workspace in order to keep itself
	 * in synch.
	 */
	void dispose();

	/**
	 * Trigger manual model refresh.
	 */
	void refresh(UserInteractions ui);

	void addElementStateListener(BootDashModel.ElementStateListener l);

	void removeElementStateListener(BootDashModel.ElementStateListener l);

	void addModelStateListener(BootDashModel.ModelStateListener l);

	void removeModelStateListener(BootDashModel.ModelStateListener l);

	BootDashViewModel getViewModel();

	void notifyElementChanged(BootDashElement element);

	RefreshState getRefreshState();

	Comparator<BootDashElement> getElementComparator();

	void notifyModelStateChanged();

	/**
	 * Gets the current name template associated with this model. This may either be
	 * a custom template set via the 'setNameTemplate' method, or it might be a
	 * template inherited from the runtarget type, or it may be null (if the runtarget
	 * type does not provide a name template.
	 *
	 * @return The effective name template or null.
	 */
	String getNameTemplate();

	/**
	 * Set a custom name template for this model. Note that this only works on models who's target provides support for
	 * persistent properties (since that's where this value is ultimately stored).
	 * <p>
	 * Setting the template to null makes the effective template be inherited from the runtarget type.
	 */
	void setNameTemplate(String template) throws Exception;

	/**
	 * @return true if this model has a custom name template (false means it inherits name template from its target type).
	 */
	boolean hasCustomNameTemplate();
}
