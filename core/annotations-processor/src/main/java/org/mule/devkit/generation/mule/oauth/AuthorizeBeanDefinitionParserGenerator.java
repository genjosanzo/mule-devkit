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
package org.mule.devkit.generation.mule.oauth;

import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.config.spring.MuleHierarchicalBeanDefinitionParserDelegate;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Variable;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;

import javax.lang.model.element.TypeElement;

public class AuthorizeBeanDefinitionParserGenerator extends AbstractMessageGenerator {
    public static final String AUTHORIZE_DEFINITION_PARSER_ROLE = "AuthorizeDefinitionParser";

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        if (typeElement.hasAnnotation(OAuth.class) || typeElement.hasAnnotation(OAuth2.class)) {
            return true;
        }

        return false;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        DefinedClass beanDefinitionParser = getAuthorizeBeanDefinitionParserClass(typeElement);
        DefinedClass messageProcessorClass = context.getClassForRole(AuthorizeMessageProcessorGenerator.AUTHORIZE_MESSAGE_PROCESSOR_ROLE);

        Method parse = beanDefinitionParser.method(Modifier.PUBLIC, ref(BeanDefinition.class), "parse");
        Variable element = parse.param(ref(org.w3c.dom.Element.class), "element");
        Variable parserContext = parse.param(ref(ParserContext.class), "parserContent");

        Variable builder = parse.body().decl(ref(BeanDefinitionBuilder.class), "builder",
                ref(BeanDefinitionBuilder.class).staticInvoke("rootBeanDefinition").arg(messageProcessorClass.dotclass().invoke("getName")));

        Variable configRef = parse.body().decl(ref(String.class), "configRef", element.invoke("getAttribute").arg("config-ref"));
        Conditional ifConfigRef = parse.body()._if(Op.cand(Op.ne(configRef, ExpressionFactory._null()),
                Op.not(ref(StringUtils.class).staticInvoke("isBlank").arg(configRef))));
        ifConfigRef._then().add(builder.invoke("addPropertyValue").arg("moduleObject").arg(
                configRef));

        Variable definition = parse.body().decl(ref(BeanDefinition.class), "definition", builder.invoke("getBeanDefinition"));

        parse.body().add(definition.invoke("setAttribute").arg(
                ref(MuleHierarchicalBeanDefinitionParserDelegate.class).staticRef("MULE_NO_RECURSE")).arg(ref(Boolean.class).staticRef("TRUE")));

        generateAttachMessageProcessor(parse, definition, parserContext);

        parse.body()._return(definition);
    }

    private DefinedClass getAuthorizeBeanDefinitionParserClass(TypeElement type) {
        String httpCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config.spring", "AuthorizeDefinitionParser");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(httpCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(httpCallbackClassName), new Class[]{BeanDefinitionParser.class});

        context.setClassRole(AUTHORIZE_DEFINITION_PARSER_ROLE, clazz);

        return clazz;
    }

}
