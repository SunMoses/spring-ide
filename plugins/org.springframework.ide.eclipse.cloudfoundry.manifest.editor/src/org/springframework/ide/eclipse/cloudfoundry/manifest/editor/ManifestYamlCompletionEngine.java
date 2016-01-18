/*******************************************************************************
 * Copyright (c) 2016 Pivotal, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Pivotal, Inc. - initial API and implementation
 *******************************************************************************/
package org.springframework.ide.eclipse.cloudfoundry.manifest.editor;

import org.springframework.ide.eclipse.editor.support.yaml.YamlDocument;
import org.springframework.ide.eclipse.editor.support.yaml.completions.TopLevelAssistContext;
import org.springframework.ide.eclipse.editor.support.yaml.completions.TypeBasedYamlCompletionEngine;
import org.springframework.ide.eclipse.editor.support.yaml.completions.YTypeAssistContext;
import org.springframework.ide.eclipse.editor.support.yaml.completions.YamlAssistContext;
import org.springframework.ide.eclipse.editor.support.yaml.structure.YamlStructureProvider;

public class ManifestYamlCompletionEngine extends TypeBasedYamlCompletionEngine {

	public ManifestYamlCompletionEngine(YamlStructureProvider structureProvider, ManifestYmlSchema schema) {
		super(structureProvider, schema.TOPLEVEL_TYPE, schema.TYPE_UTIL);
	}

	@Override
	protected YamlAssistContext getGlobalContext(YamlDocument doc) {
		return new TopLevelAssistContext() {

			@Override
			protected YamlAssistContext getDocumentContext(int documentSelector) {
				return new YTypeAssistContext(this, documentSelector, topLevelType, typeUtil);
			}
		};
	}
}
