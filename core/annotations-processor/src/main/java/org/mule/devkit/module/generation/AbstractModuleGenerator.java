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

import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.construct.FlowConstruct;
import org.mule.devkit.generation.AbstractGenerator;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Type;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.type.TypeMirror;

public abstract class AbstractModuleGenerator extends AbstractGenerator {

    public Type ref(TypeMirror typeMirror) {
        return this.context.getCodeModel().ref(typeMirror);
    }

    public TypeReference ref(Class<?> clazz) {
        return this.context.getCodeModel().ref(clazz);
    }

    public Type ref(String fullyQualifiedClassName) {
        return this.context.getCodeModel().ref(fullyQualifiedClassName);
    }

    protected Method generateSetter(DefinedClass clazz, FieldVariable field) {
        Method setter = clazz.method(Modifier.PUBLIC, context.getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Sets " + field.name());
        setter.javadoc().addParam("value Value to set");
        Variable value = setter.param(field.type(), "value");
        setter.body().assign(ExpressionFactory._this().ref(field), value);

        return setter;
    }

    protected Method generateGetter(DefinedClass clazz, FieldVariable field) {
        Method setter = clazz.method(Modifier.PUBLIC, field.type(), "get" + StringUtils.capitalize(field.name()));
        setter.javadoc().add("Retrieves " + field.name());
        setter.body()._return(ExpressionFactory._this().ref(field));

        return setter;
    }



    protected Method generateSetMuleContextMethod(DefinedClass clazz, FieldVariable muleContext) {
        Method setMuleContext = clazz.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setMuleContext");
        setMuleContext.javadoc().add("Set the Mule context");
        setMuleContext.javadoc().addParam("context Mule context to set");
        Variable muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(ExpressionFactory._this().ref(muleContext), muleContextParam);

        return setMuleContext;
    }

    protected FieldVariable generateFieldForMuleContext(DefinedClass messageProcessorClass) {
        FieldVariable muleContext =  messageProcessorClass.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");
        muleContext.javadoc().add("Mule Context");

        return muleContext;
    }


    protected Method generateSetFlowConstructMethod(DefinedClass messageSourceClass, FieldVariable flowConstruct) {
        Method setFlowConstruct = messageSourceClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "setFlowConstruct");
        setFlowConstruct.javadoc().add("Sets flow construct");
        setFlowConstruct.javadoc().addParam("flowConstruct Flow construct to set");
        Variable newFlowConstruct = setFlowConstruct.param(ref(FlowConstruct.class), "flowConstruct");
        setFlowConstruct.body().assign(ExpressionFactory._this().ref(flowConstruct), newFlowConstruct);

        return setFlowConstruct;
    }


    protected FieldVariable generateFieldForFlowConstruct(DefinedClass messageSourceClass) {
        FieldVariable flowConstruct = messageSourceClass.field(Modifier.PRIVATE, ref(FlowConstruct.class), "flowConstruct");
        flowConstruct.javadoc().add("Flow construct");
        return flowConstruct;
    }

}
