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

package org.springframework.ide.eclipse.beans.ui.graph.model;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.graph.Node;
import org.eclipse.ui.views.properties.IPropertySource;
import org.springframework.ide.eclipse.beans.core.internal.model.BeansModelUtils;
import org.springframework.ide.eclipse.beans.core.model.IBean;
import org.springframework.ide.eclipse.beans.core.model.IBeanConstructorArgument;
import org.springframework.ide.eclipse.beans.core.model.IBeanProperty;
import org.springframework.ide.eclipse.beans.ui.BeansUIUtils;

/**
 * This is a representation of a Spring bean.
 * 
 * @author Torsten Juergeleit
 */
public class Bean extends Node implements IAdaptable {

	public int preferredHeight;
	private IBean bean;

	public Bean() {
		super("empty");
	}

	public Bean(IBean bean) {
		super(bean.getElementName());
		this.bean = bean;
	}

	public IBean getBean() {
		return bean;
	}

	public String getName() {
		return (bean != null ? bean.getElementName() : "empty");
	}

	public String getClassName() {
		return bean.getClassName();
	}

	public String getParentName() {
		return bean.getParentName();
	}

	public IFile getConfigFile() {
		return (IFile) BeansModelUtils.getConfig(bean).getElementResource();
	}

	public int getStartLine() {
		return bean.getElementStartLine();
	}

	public boolean hasConstructorArguments() {
		return bean.getConstructorArguments().size() > 0;
	}

	public ConstructorArgument[] getConstructorArguments() {
		ArrayList<ConstructorArgument> list = new ArrayList<ConstructorArgument>();
		Iterator cargs = bean.getConstructorArguments().iterator();
		while (cargs.hasNext()) {
			IBeanConstructorArgument carg = (IBeanConstructorArgument) cargs
					.next();
			list.add(new ConstructorArgument(this, carg));
		}
		return list.toArray(new ConstructorArgument[list.size()]);
	}

	public boolean hasProperties() {
		return bean.getProperties().size() > 0;
	}

	public Property[] getProperties() {
		ArrayList<Property> list = new ArrayList<Property>();
		Iterator props = bean.getProperties().iterator();
		while (props.hasNext()) {
			IBeanProperty prop = (IBeanProperty) props.next();
			list.add(new Property(this, prop));
		}
		return list.toArray(new Property[list.size()]);
	}

	public boolean isRootBean() {
		return bean.isRootBean();
	}

	public boolean isChildBean() {
		return bean.isChildBean();
	}

	public Object getAdapter(Class adapter) {
		if (adapter == IPropertySource.class) {
			return BeansUIUtils.getPropertySource(bean);
		}
		return null;
	}

	@Override
	public String toString() {
		return "Bean '" + getName() + "': x=" + x + ", y=" + y + ", width=" +
			   width + ", height=" + height;
	}
}
