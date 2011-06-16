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

package org.mule.devkit.apt.generator.mule;

import org.mule.api.MuleException;
import org.mule.api.lifecycle.*;
import org.mule.devkit.annotations.lifecycle.Dispose;
import org.mule.devkit.annotations.lifecycle.Initialise;
import org.mule.devkit.annotations.lifecycle.Start;
import org.mule.devkit.annotations.lifecycle.Stop;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.*;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class LifecycleWrapperGenerator extends AbstractCodeGenerator {
    public LifecycleWrapperGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement element) throws GenerationException {
        DefinedClass lifecycleWrapper = getLifecycleWrapperClass(element);

        ExecutableElement startElement = getStartElement(element);
        if (startElement != null) {
            lifecycleWrapper._implements(Startable.class);

            generateLifecycleInvocation(lifecycleWrapper, startElement, "start", MuleException.class, false);
        }

        ExecutableElement stopElement = getStopElement(element);
        if (stopElement != null) {
            lifecycleWrapper._implements(Stoppable.class);

            generateLifecycleInvocation(lifecycleWrapper, stopElement, "stop", MuleException.class, false);
        }

        ExecutableElement initialiseElement = getInitialiseElement(element);
        if (initialiseElement != null) {
            lifecycleWrapper._implements(Initialisable.class);

            generateLifecycleInvocation(lifecycleWrapper, initialiseElement, "initialise", InitialisationException.class, true);
        }

        ExecutableElement disposeElement = getDisposeElement(element);
        if (disposeElement != null) {
            lifecycleWrapper._implements(Disposable.class);

            generateLifecycleInvocation(lifecycleWrapper, disposeElement, "dispose", null, false);
        }
    }

    private void generateLifecycleInvocation(DefinedClass lifecycleWrapper, ExecutableElement superExecutableElement, String name, Class<?> catchException, boolean addThis) {
        JMethod startMethod = lifecycleWrapper.method(JMod.PUBLIC, getContext().getCodeModel().VOID, name);

        if (catchException != null) {
            startMethod._throws(ref(catchException));
        }

        JInvocation startInvocation = JExpr._super().invoke(superExecutableElement.getSimpleName().toString());

        if (catchException != null) {
            JTryBlock tryBlock = startMethod.body()._try();
            tryBlock.body().add(startInvocation);

            int i = 0;
            for (TypeMirror exception : superExecutableElement.getThrownTypes()) {
                JCatchBlock catchBlock = tryBlock._catch(ref(exception).boxify());
                JVar catchedException = catchBlock.param("e" + i);

                JInvocation newMuleException = JExpr._new(ref(catchException));
                newMuleException.arg(catchedException);

                if (addThis) {
                    newMuleException.arg(JExpr._this());
                }

                catchBlock.body().add(newMuleException);
                i++;
            }
        } else {
            startMethod.body().add(startInvocation);
        }
    }

    private ExecutableElement getStartElement(TypeElement element) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Start start = executableElement.getAnnotation(Start.class);

            if (start != null) {
                return executableElement;
            }
        }

        return null;
    }

    private ExecutableElement getStopElement(TypeElement element) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Stop stop = executableElement.getAnnotation(Stop.class);

            if (stop != null) {
                return executableElement;
            }
        }

        return null;
    }

    private ExecutableElement getInitialiseElement(TypeElement element) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Initialise initialise = executableElement.getAnnotation(Initialise.class);

            if (initialise != null) {
                return executableElement;
            }
        }

        return null;
    }

    private ExecutableElement getDisposeElement(TypeElement element) {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(element.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Dispose dispose = executableElement.getAnnotation(Dispose.class);

            if (dispose != null) {
                return executableElement;
            }
        }

        return null;
    }
}
