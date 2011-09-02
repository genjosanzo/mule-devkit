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

import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.InterceptCallback;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class InterceptCallbackGenerator extends AbstractModuleGenerator {

    public static final String ROLE = "InterceptCallback";

    @Override
    protected boolean shouldGenerate(TypeElement typeElement) {
        List<ExecutableElement> methods = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Processor processor = method.getAnnotation(Processor.class);
            if (processor != null && processor.intercepting()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void doGenerate(TypeElement typeElement) {
        DefinedClass callbackClass = getInterceptCallbackClass(typeElement);
        callbackClass._implements(ref(InterceptCallback.class));

        FieldVariable shallContinue = callbackClass.field(Modifier.PRIVATE, context.getCodeModel().BOOLEAN, "shallContinue", ExpressionFactory.TRUE);

        generateGetter(callbackClass, shallContinue);

        Method doNotContinue = callbackClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "doNotContinue");
        doNotContinue.body().assign(shallContinue, ExpressionFactory.FALSE);

        context.setClassRole(ROLE, callbackClass);
    }

    private DefinedClass getInterceptCallbackClass(TypeElement type) {
        String sourceCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config.spring", "InterceptCallback");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(sourceCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(sourceCallbackClassName));

        context.setClassRole(ROLE, clazz);

        return clazz;
    }
}