package org.mule.devkit.apt.generator.mule;

import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;
import org.mule.api.MuleException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.devkit.annotations.lifecycle.Dispose;
import org.mule.devkit.annotations.lifecycle.Initialise;
import org.mule.devkit.annotations.lifecycle.Start;
import org.mule.devkit.annotations.lifecycle.Stop;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;

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
        JDefinedClass lifecycleWrapper = getLifecycleWrapperClass(element);

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

    private void generateLifecycleInvocation(JDefinedClass lifecycleWrapper, ExecutableElement superExecutableElement, String name, Class<?> catchException, boolean addThis) {
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
