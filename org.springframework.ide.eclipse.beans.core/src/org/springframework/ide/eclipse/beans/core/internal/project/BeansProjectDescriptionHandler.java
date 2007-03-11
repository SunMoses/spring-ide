/*
 * Copyright 2002-2007 the original author or authors.
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

import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.springframework.ide.eclipse.beans.core.BeansCorePlugin;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansConfigSet;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansProject;
import org.springframework.ide.eclipse.beans.core.model.IBeansProject;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This class provides a SAX handler for a Spring project's description file.
 * 
 * @author Torsten Juergeleit
 */
public class BeansProjectDescriptionHandler extends DefaultHandler implements
		IBeansProjectDescriptionConstants {
	protected enum State { INITIAL, PROJECT_DESC, CONFIG_EXTENSIONS,
		CONFIG_EXTENSION, CONFIGS, CONFIG, CONFIG_SETS, CONFIG_SET,
		CONFIG_SET_NAME, CONFIG_SET_OVERRIDING, CONFIG_SET_INCOMPLETE,
		CONFIG_SET_CONFIGS,CONFIG_SET_CONFIG
	}
	protected BeansProject project;
	protected MultiStatus problems;
	protected State state;
	protected BeansConfigSet configSet;

	protected final StringBuffer charBuffer = new StringBuffer();
	protected Locator locator;

	public BeansProjectDescriptionHandler(BeansProject project) {
		this.project = project;
		problems = new MultiStatus(BeansCorePlugin.PLUGIN_ID,
				IResourceStatus.FAILED_READ_METADATA,
				"Error reading Spring project description", null);
		state = State.INITIAL;
	}

	public IStatus getStatus() {
		return problems;
	}

	@Override
	public void startElement(String uri, String elementName, String qname,
			Attributes attributes) throws SAXException {
		// clear the character buffer at the start of every element
		charBuffer.setLength(0);
		if (state == State.INITIAL) {
			if (elementName.equals(PROJECT_DESCRIPTION)) {
				state = State.PROJECT_DESC;
			} else {
				throw new SAXParseException("No Spring project description",
						locator);
			}
		} else if (state == State.PROJECT_DESC) {
			if (elementName.equals(CONFIG_EXTENSIONS)) {
				state = State.CONFIG_EXTENSIONS;
			} else if (elementName.equals(CONFIGS)) {
				state = State.CONFIGS;
			} else if (elementName.equals(CONFIG_SETS)) {
				state = State.CONFIG_SETS;
			}
		} else if (state == State.CONFIG_EXTENSIONS) {
			if (elementName.equals(CONFIG_EXTENSION)) {
				state = State.CONFIG_EXTENSION;
			}
		} else if (state == State.CONFIGS) {
			if (elementName.equals(CONFIG)) {
				state = State.CONFIG;
			}
		} else if (state == State.CONFIG_SETS) {
			if (elementName.equals(CONFIG_SET)) {
				state = State.CONFIG_SET;
			}
		} else if (state == State.CONFIG_SET) {
			if (elementName.equals(NAME)) {
				state = State.CONFIG_SET_NAME;
			} else if (elementName.equals(OVERRIDING)) {
				state = State.CONFIG_SET_OVERRIDING;
			} else if (elementName.equals(INCOMPLETE)) {
				state = State.CONFIG_SET_INCOMPLETE;
			} else if (elementName.equals(CONFIGS)) {
				state = State.CONFIG_SET_CONFIGS;
			}
		} else if (state == State.CONFIG_SET_CONFIGS) {
			if (elementName.equals(CONFIG)) {
				state = State.CONFIG_SET_CONFIG;
			}
		}
	}

	@Override
	public void endElement(String uri, String elementName, String qname)
			throws SAXException {
		if (state == State.PROJECT_DESC) {

			// make sure that at least the default config extension is in
			// the list of config extensions
			if (project.getConfigExtensions().isEmpty()) {
				project.addConfigExtension(IBeansProject
						.DEFAULT_CONFIG_EXTENSION);
			}
		} else if (state == State.CONFIG_EXTENSIONS) {
			if (elementName.equals(CONFIG_EXTENSIONS)) {
				state = State.PROJECT_DESC;
			}
		} else if (state == State.CONFIG_EXTENSION) {
			if (elementName.equals(CONFIG_EXTENSION)) {
				String extension = charBuffer.toString().trim();
				project.addConfigExtension(extension);
				state = State.CONFIG_EXTENSIONS;
			}
		} else if (state == State.CONFIGS) {
			if (elementName.equals(CONFIGS)) {
				state = State.PROJECT_DESC;
			}
		} else if (state == State.CONFIG) {
			if (elementName.equals(CONFIG)) {
				String config = charBuffer.toString().trim();

				// If given config is a full path within this Spring
				// project then convert it to a project relative path
				if (config.length() > 0 && config.charAt(0) == '/') {
					String projectPath = '/' + project.getElementName() + '/';
					if (config.startsWith(projectPath)) {
						config = config.substring(projectPath.length());
					}
				}
				project.addConfig(config);
				state = State.CONFIGS;
			}
		} else if (state == State.CONFIG_SETS) {
			if (elementName.equals(CONFIG_SETS)) {
				state = State.PROJECT_DESC;
			}
		} else if (state == State.CONFIG_SET) {
			if (elementName.equals(CONFIG_SET)) {
				project.addConfigSet(configSet);
				state = State.CONFIG_SETS;
			}
		} else if (state == State.CONFIG_SET_NAME) {
			if (elementName.equals(NAME)) {
				String name = charBuffer.toString().trim();
				configSet = new BeansConfigSet(project, name);
				state = State.CONFIG_SET;
			}
		} else if (state == State.CONFIG_SET_OVERRIDING) {
			if (elementName.equals(OVERRIDING)) {
				boolean override = Boolean
						.valueOf(charBuffer.toString().trim()).booleanValue();
				configSet.setAllowBeanDefinitionOverriding(override);
				state = State.CONFIG_SET;
			}
		} else if (state == State.CONFIG_SET_INCOMPLETE) {
			if (elementName.equals(INCOMPLETE)) {
				boolean incomplete = Boolean.valueOf(
						charBuffer.toString().trim()).booleanValue();
				configSet.setIncomplete(incomplete);
				state = State.CONFIG_SET;
			}
		} else if (state == State.CONFIG_SET_CONFIGS) {
			if (elementName.equals(CONFIGS)) {
				state = State.CONFIG_SET;
			}
		} else if (state == State.CONFIG_SET_CONFIG) {
			if (elementName.equals(CONFIG)) {
				String config = charBuffer.toString().trim();

				// If given config is a full path within this Spring
				// project then convert it to a project relative path
				if (config.length() > 0 && config.charAt(0) == '/') {
					String projectPath = '/' + project.getElementName() + '/';
					if (config.startsWith(projectPath)) {
						config = config.substring(projectPath.length());
					}
				}
				configSet.addConfig(config);
				state = State.CONFIG_SET_CONFIGS;
			}
		}
		charBuffer.setLength(0);
	}

	@Override
	public void characters(char[] chars, int offset, int length)
			throws SAXException {
		// accumulate characters and process them when endElement is reached
		charBuffer.append(chars, offset, length);
	}

	@Override
	public void setDocumentLocator(Locator locator) {
		this.locator = locator;
	}

	@Override
	public void error(SAXParseException error) throws SAXException {
		log(IStatus.WARNING, error);
	}

	@Override
	public void warning(SAXParseException error) throws SAXException {
		log(IStatus.WARNING, error);
	}

	public void log(int code, Throwable error) {
		log(code, error.getMessage(), error);
	}

	public void log(int code, String errorMessage, Throwable error) {
		problems.add(new Status(code, BeansCorePlugin.PLUGIN_ID,
				IResourceStatus.FAILED_READ_METADATA, errorMessage, error));
	}
}
