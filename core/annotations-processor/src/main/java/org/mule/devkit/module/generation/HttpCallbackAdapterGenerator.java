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

package org.mule.devkit.module.generation;

import org.mule.api.annotations.callback.HttpCallback;
import org.mule.api.annotations.oauth.OAuth;
import org.mule.api.annotations.oauth.OAuth2;
import org.mule.api.lifecycle.Initialisable;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Package;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.builders.FieldBuilder;
import org.mule.util.NumberUtils;

import javax.lang.model.element.TypeElement;

public class HttpCallbackAdapterGenerator extends AbstractModuleGenerator {

    public static final String PORT_FIELD_NAME = "port";
    public static final String DOMAIN_FIELD_NAME = "domain";

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        return typeElement.hasAnnotation(OAuth.class) ||
               typeElement.hasAnnotation(OAuth2.class) ||
               typeElement.hasProcessorMethodWithParameter(HttpCallback.class);
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) {
        DefinedClass httpCallbackAdapter = getHttpCallbackAdapterClass(typeElement);
        FieldVariable port = portFieldWithGetterAndSetter(httpCallbackAdapter);
        FieldVariable domain = domainFieldWithGetterAndSetter(httpCallbackAdapter);
        FieldVariable logger = FieldBuilder.newLoggerField(httpCallbackAdapter);
        generateInitialiseMethod(httpCallbackAdapter, port, domain, logger);
    }

    private void generateInitialiseMethod(DefinedClass httpCallbackAdapter, FieldVariable port, FieldVariable domain, FieldVariable logger) {
        Method initialise = httpCallbackAdapter.method(Modifier.PUBLIC, this.context.getCodeModel().VOID, "initialise");
        if(ref(Initialisable.class).isAssignableFrom(httpCallbackAdapter._extends())) {
            initialise.body().invoke(ExpressionFactory._super(), "initialise");
        }

        Block ifPortIsNull = initialise.body()._if(Op.eq(port, ExpressionFactory._null()))._then();
        assignHttpPortSystemVariable(port, logger, ifPortIsNull);

        Block ifDomainIsNull = initialise.body()._if(Op.eq(domain, ExpressionFactory._null()))._then();
        assignDomainSystemVariable(domain, logger, ifDomainIsNull);
    }

    private void assignHttpPortSystemVariable(FieldVariable port, FieldVariable logger, Block ifPortIsNull) {
        Variable portSystemVar = ifPortIsNull.decl(ref(String.class), "portSystemVar", ref(System.class).staticInvoke("getProperty").arg("http.port"));
        Conditional conditional = ifPortIsNull._if(ref(NumberUtils.class).staticInvoke("isDigits").arg(portSystemVar));
        conditional._then().block().assign(port, ref(Integer.class).staticInvoke("parseInt").arg(portSystemVar));
        Block thenBlock = conditional._else().block();
        thenBlock.invoke(logger, "warn").arg(ExpressionFactory.lit("Environment variable 'http.port' not found, using default port: 8080"));
        thenBlock.assign(port, ExpressionFactory.lit(8080));
    }

    private void assignDomainSystemVariable(FieldVariable domain, FieldVariable logger, Block ifDomainIsNull) {
        Variable domainSystemVar = ifDomainIsNull.decl(ref(String.class), "domainSystemVar", ref(System.class).staticInvoke("getProperty").arg("fullDomain"));
        Conditional conditional = ifDomainIsNull._if(Op.ne(domainSystemVar, ExpressionFactory._null()));
        conditional._then().block().assign(domain, domainSystemVar);
        Block thenBlock = conditional._else().block();
        thenBlock.invoke(logger, "warn").arg("Environment variable 'fullDomain' not found, using default: localhost");
        thenBlock.assign(domain, ExpressionFactory.lit("localhost"));
    }

    private DefinedClass getHttpCallbackAdapterClass(TypeElement typeElement) {
        String httpCallbackAdapterClassName = context.getNameUtils().generateClassName(typeElement, ".config", "HttpCallbackAdapter");
        Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(httpCallbackAdapterClassName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass oauthAdapter = pkg._class(context.getNameUtils().getClassName(httpCallbackAdapterClassName), classToExtend);
        oauthAdapter._implements(ref(Initialisable.class));

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), oauthAdapter);

        return oauthAdapter;
    }

    private FieldVariable portFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(Integer.class).name(PORT_FIELD_NAME).getterAndSetter().build();
    }

    private FieldVariable domainFieldWithGetterAndSetter(DefinedClass oauthAdapter) {
        return new FieldBuilder(oauthAdapter).type(String.class).name(DOMAIN_FIELD_NAME).getterAndSetter().build();
    }
}