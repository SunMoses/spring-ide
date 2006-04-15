/*
 * Copyright 2002-2006 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package org.springframework.ide.eclipse.beans.core.internal.project;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansConfig;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansConfigSet;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfig;
import org.springframework.ide.eclipse.beans.core.model.IBeansConfigSet;
import org.springframework.ide.eclipse.beans.core.model.IBeansProject;

/**
 * This class holds the configuration for a Spring Beans project.
 *
 * @author Torsten Juergeleit
 */
public class BeansProjectDescription {

	private IBeansProject project;
	private Set configExtensions;
	private Set configNames;
	private Map configs;
	private Map configSets;

	public BeansProjectDescription(IBeansProject project) {
		this.project = project;
		this.configExtensions = new HashSet();
		this.configs = new HashMap();
		this.configNames = new HashSet();
		this.configSets = new HashMap();
	}
	
	public Set getConfigExtensions() {
		return Collections.unmodifiableSet(configExtensions);
	}
	
	public void setConfigExtensions(Set configExtensions) {
		this.configExtensions = configExtensions;
	}

	public void addConfigExtension(String extension) {
		if (extension.length() > 0 && !configExtensions.contains(extension)) {
			configExtensions.add(extension);
		}
	}

	public void setConfigNames(Collection configNames) {
		this.configNames = new HashSet(configNames);
		this.configs = new HashMap();
		Iterator iter = this.configNames.iterator();
		while (iter.hasNext()) {
			String configName = (String) iter.next();
			IBeansConfig config = new BeansConfig(project, configName);
			configs.put(configName, config);
		}
	}

	public Set getConfigNames() {
		return Collections.unmodifiableSet(configNames);
	}

	public boolean addConfig(IFile file) {
		return addConfig(file.getProjectRelativePath().toString());
	}

	public boolean addConfig(String name) {
		if (name.length() > 0 && !configNames.contains(name)) {
			configNames.add(name);
			IBeansConfig config = new BeansConfig(project, name);
			configs.put(name, config);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if given file belongs to the list of Spring bean config
	 * files which are stored in the project description. 
	 */
	public boolean hasConfig(IFile file) {
		return configNames.contains(file.getProjectRelativePath().toString());
	}

	/**
	 * Returns true if given config (project-relative file name) belongs to the
	 * list of Spring bean config files which are stored in the project
	 * description. 
	 */
	public boolean hasConfig(String name) {
		return configNames.contains(name);
	}

	public IBeansConfig getConfig(IFile file) {
		String name = file.getProjectRelativePath().toString();
		if (configNames.contains(name)) {
			return (IBeansConfig) configs.get(name);
		}
		return null;
	}

	public IBeansConfig getConfig(String name) {
		if (configNames.contains(name)) {
			return (IBeansConfig) configs.get(name);
		}
		return null;
	}

	public Collection getConfigs() {
		return Collections.unmodifiableCollection(configs.values());
	}

	public boolean removeConfig(IFile file) {
		return removeConfig(file.getProjectRelativePath().toString());
	}
	
	public boolean removeConfig(String name) {
		if (hasConfig(name)) {
			configNames.remove(name);
			configs.remove(name);
			removeConfigFromConfigSets(name);
			return true;
		}
		return false;
	}

	public boolean removeExternalConfig(IFile file) {
		return removeConfigFromConfigSets(file.getFullPath().toString());
	}

	private boolean removeConfigFromConfigSets(String name) {
		Iterator iter = configSets.values().iterator();
		while (iter.hasNext()) {
			BeansConfigSet configSet = (BeansConfigSet) iter.next();
			if (configSet.hasConfig(name)) {
				configSet.removeConfig(name);
				return true;
			}
		}
		return false;
	}

	public void addConfigSet(IBeansConfigSet configSet) {
		configSets.put(configSet.getElementName(), configSet);
	}

	public void setConfigSets(List configSets) {
		this.configSets.clear();
		Iterator iter = configSets.iterator();
		while (iter.hasNext()) {
			IBeansConfigSet configSet = (IBeansConfigSet) iter.next();
			this.configSets.put(configSet.getElementName(), configSet);
		}
	}

	public int getNumberOfConfigSets() {
		return configSets.size();
	}

	public Collection getConfigSetNames() {
		return Collections.unmodifiableCollection(configSets.keySet());
	}

	public IBeansConfigSet getConfigSet(String name) {
		return (IBeansConfigSet) configSets.get(name);
	}

	public Collection getConfigSets() {
		return Collections.unmodifiableCollection(configSets.values());
	}

	public String toString() {
		return "Configs=" + configNames + ", ConfigsSets=" + configSets.toString();
	}
}
