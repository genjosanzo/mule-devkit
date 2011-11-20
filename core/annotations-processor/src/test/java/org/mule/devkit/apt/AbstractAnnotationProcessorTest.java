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

package org.mule.devkit.apt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ClassUtils;

import javax.annotation.processing.Processor;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * A base test class for {@link Processor annotation processor} testing that
 * attempts to compile source test cases that can be found on the classpath.
 */
public abstract class AbstractAnnotationProcessorTest {

    private static final String SOURCE_FILE_SUFFIX = ".java";
    private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
    protected DiagnosticCollector<JavaFileObject> diagnosticCollector = new DiagnosticCollector<JavaFileObject>();

    /**
     * @return the processor instances that should be tested
     */
    protected abstract Collection<Processor> getProcessors();

    /**
     * Attempts to compile the given compilation units using the Java Compiler
     * API.
     * <p/>
     * The compilation units and all their dependencies are expected to be on
     * the classpath.
     *
     * @param compilationUnits the classes to compile
     */
    protected void compileTestCase(Class<?>... compilationUnits) {
        if (compilationUnits == null || compilationUnits.length == 0) {
            throw new IllegalArgumentException("No compilation units specified");
        }

        String[] compilationUnitPaths = new String[compilationUnits.length];

        for (int i = 0; i < compilationUnitPaths.length; i++) {
            if (compilationUnits[i] == null) {
                throw new IllegalArgumentException("Compilation unit cannot be null");
            }
            compilationUnitPaths[i] = toResourcePath(compilationUnits[i]);
        }

        compileTestCase(compilationUnitPaths);
    }

    private static String toResourcePath(Class<?> clazz) {
        return ClassUtils.convertClassNameToResourcePath(clazz.getName()) + SOURCE_FILE_SUFFIX;
    }

    /**
     * Attempts to compile the given compilation units using the Java Compiler API.
     * <p/>
     * The compilation units and all their dependencies are expected to be on the classpath.
     *
     * @param compilationUnitPaths the paths of the source files to compile, as would be expected
     *                             by {@link ClassLoader#getResource(String)}
     * @return the {@link Diagnostic diagnostics} returned by the compilation,
     *         as demonstrated in the documentation for {@link JavaCompiler}
     * @see #compileTestCase(Class...)
     */
    protected void compileTestCase(String... compilationUnitPaths) {
        if (compilationUnitPaths == null || compilationUnitPaths.length == 0) {
            throw new IllegalArgumentException("No compilation unit path specified");
        }

        Collection<File> compilationUnits;

        try {
            compilationUnits = findClasspathFiles(compilationUnitPaths);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to resolve compilation units " + Arrays.toString(compilationUnitPaths) +
                    " due to: " + exception.getMessage(), exception);
        }

        StandardJavaFileManager fileManager = COMPILER.getStandardFileManager(diagnosticCollector, null, null);

        /*
         * Call the compiler with the "-proc:only" option. The "class names"
         * option (which could, in principle, be used instead of compilation
         * units for annotation processing) isn't useful in this case because
         * only annotations on the classes being compiled are accessible.
         *
         * Information about the classes being compiled (such as what they are annotated
         * with) is *not* available via the RoundEnvironment. However, if these classes
         * are annotations, they certainly need to be validated.
         */
        CompilationTask task = COMPILER.getTask(null, fileManager, diagnosticCollector,
                Arrays.asList("-proc:only"), null,
                fileManager.getJavaFileObjectsFromFiles(compilationUnits));
        task.setProcessors(getProcessors());
        task.call();

        closeQuietly(fileManager);
    }

    private static Collection<File> findClasspathFiles(String[] filenames) throws IOException {
        Collection<File> classpathFiles = new ArrayList<File>(filenames.length);

        for (String filename : filenames) {
            classpathFiles.add(new ClassPathResource(filename).getFile());
        }

        return classpathFiles;
    }

    /**
     * Asserts that the compilation produced no errors, i.e. no diagnostics of
     * type {@link Kind#ERROR}.
     *
     */
    protected void assertCompilationSuccessful() {
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            assertNotSame("Error not expected at line: " + diagnostic.getLineNumber(), Kind.ERROR, diagnostic.getKind());
        }
    }

    /**
     * Asserts that the compilation produced at least one error.
     */
    protected void assertCompilationFailed() {
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnosticCollector.getDiagnostics()) {
            if (diagnostic.getKind() == Kind.ERROR) {
                return;
            }
        }
        fail("No error found in compilation");
    }

    /**
     * Asserts that the compilation produced results of the following
     * {@link Kind Kinds} at the given line numbers, where the <em>n</em>th kind
     * is expected at the <em>n</em>th line number.
     * <p/>
     * Does not check that these is the <em>only</em> diagnostic kinds returned!
     *
     * @param expectedDiagnosticKinds the kinds of diagnostic expected
     * @param expectedLineNumbers     the line numbers at which the diagnostics are expected
     * @param diagnostics             the result of the compilation
     * @see #assertCompilationReturned(Kind, long, List)
     */
    protected static void assertCompilationReturned(
            Kind[] expectedDiagnosticKinds, long[] expectedLineNumbers,
            List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        assert ((expectedDiagnosticKinds != null) && (expectedLineNumbers != null)
                && (expectedDiagnosticKinds.length == expectedLineNumbers.length));

        for (int i = 0; i < expectedDiagnosticKinds.length; i++) {
            assertCompilationReturned(expectedDiagnosticKinds[i], expectedLineNumbers[i],
                    diagnostics);
        }

    }

    /**
     * Asserts that the compilation produced a result of the following
     * {@link Kind} at the given line number.
     * <p/>
     * Does not check that this is the <em>only</em> diagnostic kind returned!
     *
     * @param expectedDiagnosticKind the kind of diagnostic expected
     * @param expectedLineNumber     the line number at which the diagnostic is expected
     * @param diagnostics            the result of the compilation
     * @see #assertCompilationReturned(Kind[], long[], List)
     */
    protected static void assertCompilationReturned(Kind expectedDiagnosticKind, long expectedLineNumber, List<Diagnostic<? extends JavaFileObject>> diagnostics) {
        if (expectedDiagnosticKind == null || diagnostics == null) {
            throw new IllegalArgumentException("Diagnostic kind and diagnostics must be specified");
        }
        boolean expectedDiagnosticFound = false;

        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics) {

            if (diagnostic.getKind() == expectedDiagnosticKind && diagnostic.getLineNumber() == expectedLineNumber) {
                expectedDiagnosticFound = true;
            }

        }

        assertTrue(diagnostics.toString(), expectedDiagnosticFound);
    }

    private void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (IOException e) {
            // ignore
        }
    }
}