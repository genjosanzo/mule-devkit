/**
 * Mule Development Kit
 * Copyright 2010-2011 (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mule.devkit.generation.mule.studio;

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.Transformer;
import org.mule.devkit.GeneratorContext;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.model.studio.AttributeCategory;
import org.mule.devkit.model.studio.PatternType;

import javax.lang.model.element.ExecutableElement;
import java.util.Collection;

public class PatternTypeBuilder extends BaseStudioXmlBuilder {

    public PatternTypeBuilder(GeneratorContext context, ExecutableElement executableElement, DevKitTypeElement typeElement) {
        super(context, executableElement, typeElement);
    }

    public PatternType build() {
        PatternType patternType = createPatternType();
        if (executableElement.getAnnotation(Processor.class) != null) {
            Collection<AttributeCategory> attributeCategories = processMethodParameters();
            patternType.getAttributeCategoryOrRequiredSetAlternativesOrFixedAttribute().addAll(attributeCategories);
        }
        return patternType;
    }

    private PatternType createPatternType() {
        PatternType cloudConnector = new PatternType();
        cloudConnector.setLocalId(nameUtils.uncamel(executableElement.getSimpleName().toString()));
        cloudConnector.setCaption(helper.formatCaption(nameUtils.uncamel(executableElement.getSimpleName().toString())));
        cloudConnector.setAbstract(executableElement.getAnnotation(Processor.class) != null);

        if (executableElement.getAnnotation(Processor.class) != null) {
            cloudConnector.setExtends(MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + helper.getGlobalRefId(typeElement.name()));
        } else if (executableElement.getAnnotation(Transformer.class) != null) {
            cloudConnector.setExtends(MuleStudioXmlGenerator.URI_PREFIX + typeElement.name() + '/' + AbstractTransformerBuilder.ABSTRACT_TRANSFORMER_LOCAL_ID);
            cloudConnector.setDescription(helper.formatDescription(javaDocUtils.getSummary(executableElement)));
        }

        cloudConnector.setIcon(helper.getIcon(typeElement.name()));
        cloudConnector.setImage(helper.getImage(typeElement.name()));
        return cloudConnector;
    }
}