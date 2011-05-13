package org.mule.devkit.apt;

import org.junit.Test;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import java.util.Arrays;
import java.util.Collection;

public class ModuleAnnotationProcessorTest extends AbstractAnnotationProcessorTest {
    @Override
    protected Collection<Processor> getProcessors() {
        return Arrays.<Processor> asList(new ModuleAnnotationProcessor());
    }

    /*
    @Test
    public void successfulCompilation() {
        assertCompilationSuccessful(compileTestCase(BasicModule.class));
    }
    */

    @Test
    public void moduleAnnotationAtInterfaceInvalid() {
        assertCompilationReturned(Diagnostic.Kind.ERROR, 6, compileTestCase(InvalidTypeInterface.class));
    }

    @Test
    public void moduleAnnotationAtGenericInvalid() {
        assertCompilationReturned(Diagnostic.Kind.ERROR, 6, compileTestCase(InvalidTypeGeneric.class));
    }

    @Test
    public void moduleAnnotationPrivateInvalid() {
        assertCompilationReturned(Diagnostic.Kind.ERROR, 6, compileTestCase(InvalidTypeNonPublic.class));
    }

    @Test
    public void fieldAnnotationFinalInvalid() {
        assertCompilationReturned(Diagnostic.Kind.ERROR, 9, compileTestCase(InvalidFieldFinal.class));
    }
}
