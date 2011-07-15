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

import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.api.annotations.Processor;
import org.mule.api.annotations.callback.ProcessorCallback;
import org.mule.api.context.MuleContextAware;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorBuilder;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.ForEach;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Variable;
import org.mule.processor.chain.DefaultMessageProcessorChainBuilder;
import org.springframework.beans.factory.FactoryBean;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class ProcessorCallbackFactoryGenerator extends AbstractModuleGenerator {
    public static final String FACTORY_ROLE = "ProcessorCallbackFactory";
    public static final String CALLBACK_ROLE = "ProcessorCallback";

    public void generate(Element element) throws GenerationException {
        boolean shouldGenerate = false;

        java.util.List<ExecutableElement> methods = ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Processor processor = method.getAnnotation(Processor.class);
            if (processor == null)
                continue;

            for (VariableElement variable : method.getParameters()) {
                if (variable.asType().toString().contains(ProcessorCallback.class.getName())) {
                    shouldGenerate = true;
                    break;
                }
            }
        }

        if (shouldGenerate) {
            // get class
            DefinedClass callbackFactoryClass = getProcessorCallbackClass((TypeElement) element);

            // define inner class
            DefinedClass callbackClass = generateCallbackClass(callbackFactoryClass);

            // declare object
            FieldVariable muleContext = generateFieldForMuleContext(callbackFactoryClass);

            // add setmulecontext
            generateSetMuleContextMethod(callbackFactoryClass, muleContext);

            FieldVariable messageProcessors = callbackFactoryClass.field(Modifier.PRIVATE, ref(List.class).narrow(ref(MessageProcessor.class)), "messageProcessors");

            generateSetter(callbackFactoryClass, messageProcessors);

            Method getObjectType = callbackFactoryClass.method(Modifier.PUBLIC, ref(Class.class), "getObjectType");
            getObjectType.body()._return(ref(ProcessorCallback.class).dotclass());

            Method isSingleton = callbackFactoryClass.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "isSingleton");
            isSingleton.body()._return(ExpressionFactory.FALSE);

            Method getObject = callbackFactoryClass.method(Modifier.PUBLIC, ref(Object.class), "getObject");
            getObject._throws(Exception.class);

            Variable builder = getObject.body().decl(ref(MessageProcessorChainBuilder.class), "builder", ExpressionFactory._new(ref(DefaultMessageProcessorChainBuilder.class)));
            ForEach forEachProcessor = getObject.body().forEach(ref(Object.class), "processor", messageProcessors);
            Conditional isProcessor = forEachProcessor.body()._if(Op._instanceof(forEachProcessor.var(), ref(MessageProcessor.class)));
            isProcessor._then().add(builder.invoke("chain").arg(ExpressionFactory.cast(ref(MessageProcessor.class), forEachProcessor.var())));
            Conditional isProcessorBuilder = isProcessor._elseif(Op._instanceof(forEachProcessor.var(), ref(MessageProcessorBuilder.class)));
            isProcessorBuilder._then().add(builder.invoke("chain").arg(ExpressionFactory.cast(ref(MessageProcessorBuilder.class), forEachProcessor.var())));
            isProcessorBuilder._else()._throw(ExpressionFactory._new(ref(IllegalArgumentException.class)).arg(
                    "MessageProcessorBuilder should only have MessageProcessor's or MessageProcessorBuilder's configured"
            ));

            getObject.body()._return(ExpressionFactory._new(callbackClass).arg(builder.invoke("build")));
        }
    }

    private DefinedClass generateCallbackClass(DefinedClass callbackFactoryClass) {
        DefinedClass callbackClass = callbackFactoryClass._class("ProcessorCallback");
        callbackClass._implements(ref(Cloneable.class));
        callbackClass._implements(ref(ProcessorCallback.class));

        FieldVariable muleContext = callbackClass.field(Modifier.PRIVATE, ref(MuleContext.class), "muleContext");
        muleContext.javadoc().add("Mule Context");

        generateSetter(callbackClass, muleContext);

        FieldVariable chain = callbackClass.field(Modifier.PRIVATE, ref(MessageProcessorChain.class), "chain");
        chain.javadoc().add("Chain that will be executed upon calling process");

        generateSetter(callbackClass, chain);

        FieldVariable event = callbackClass.field(Modifier.PRIVATE, ref(MuleEvent.class), "event");
        event.javadoc().add("Event that will be cloned for dispatching");

        generateSetter(callbackClass, event);

        generateCallbackConstructor(callbackClass, chain);

        generateCallbackProcess(callbackClass, chain, event, muleContext);

        generateCallbackClone(callbackClass, chain);

        context.setClassRole(CALLBACK_ROLE, callbackClass);

        return callbackClass;
    }

    private void generateCallbackClone(DefinedClass callbackClass, FieldVariable chain) {
        Method clone = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "clone");
        clone._throws(ref(CloneNotSupportedException.class));

        Variable clonedObject = clone.body().decl(callbackClass, "clone",
                ExpressionFactory.cast(callbackClass, ExpressionFactory._super().invoke("clone")));

        clone.body().assign(clonedObject.ref("chain"), ExpressionFactory._this().ref(chain));
        clone.body()._return(clonedObject);
    }

    private void generateCallbackProcess(DefinedClass callbackClass, FieldVariable chain, FieldVariable event, FieldVariable muleContext) {
        Method process = callbackClass.method(Modifier.PUBLIC, ref(Object.class), "process");
        process._throws(ref(Exception.class));
        Variable payload = process.param(ref(Object.class), "payload");

        Variable muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        Invocation newMuleMessage = ExpressionFactory._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(payload);
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        Variable muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        Invocation newMuleEvent = ExpressionFactory._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(event);
        process.body().assign(muleEvent, newMuleEvent);

        process.body()._return(chain.invoke("process").arg(muleEvent).invoke("getMessage").invoke("getPayload"));
    }

    private void generateCallbackConstructor(DefinedClass callbackClass, FieldVariable chain) {
        Method constructor = callbackClass.constructor(Modifier.PUBLIC);
        Variable chain2 = constructor.param(ref(MessageProcessorChain.class), "chain");
        constructor.body().assign(ExpressionFactory._this().ref(chain), chain2);
    }

    private DefinedClass getProcessorCallbackClass(TypeElement type) {
        String processorCallbackClassName = context.getNameUtils().generateClassNameInPackage(type, ".config.spring", "ProcessorCallbackFactory");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(processorCallbackClassName));
        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(processorCallbackClassName), new Class[]{MuleContextAware.class, FactoryBean.class});

        context.setClassRole(FACTORY_ROLE, clazz);

        return clazz;
    }
}
