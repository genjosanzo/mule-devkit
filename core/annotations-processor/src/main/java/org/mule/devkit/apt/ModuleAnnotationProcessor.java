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

import org.mule.devkit.GeneratorContext;
import org.mule.devkit.Plugin;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.generation.DevkitTypeElementImpl;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.generation.Generator;
import org.mule.devkit.validation.ValidationException;
import org.mule.devkit.validation.Validator;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

@SupportedAnnotationTypes(value = {"org.mule.api.annotations.Module"})
@SupportedSourceVersion(SourceVersion.RELEASE_6)
public class ModuleAnnotationProcessor extends AbstractProcessor {

    private GeneratorContext context;
    private List<Plugin> plugins;

    /**
     * Gets all the {@link Plugin}s discovered so far.
     */
    public List<Plugin> getPlugins() {
        if (plugins == null) {
            plugins = new ArrayList<Plugin>();
            ClassLoader ucl = getUserClassLoader(getClass().getClassLoader());
            plugins.addAll(findServices(Plugin.class, ucl));
        }

        return plugins;
    }

    /**
     * Gets a classLoader that can load classes specified via the
     * -classpath option.
     */
    public URLClassLoader getUserClassLoader(ClassLoader parent) {
        String classpath = processingEnv.getOptions().get("-cp");

        if (classpath == null) {
            classpath = processingEnv.getOptions().get("-classpath");
        }

        List<URL> classpaths = new ArrayList<URL>();

        if (classpath != null) {
            for (String p : classpath.split(File.pathSeparator)) {
                File file = new File(p);
                try {
                    classpaths.add(file.toURI().toURL());
                } catch (MalformedURLException e) {
                    warn(e.getMessage());
                }
            }
        }

        return new URLClassLoader(classpaths.toArray(new URL[classpaths.size()]), parent);
    }

    /**
     * Looks for all "META-INF/services/[className]" files and
     * create one instance for each class name found inside this file.
     */
    private static <T> List<T> findServices(Class<T> clazz, ClassLoader classLoader) {

        Iterable<T> itr = ServiceLoader.load(clazz, classLoader);
        List<T> r = new ArrayList<T>();
        for (T t : itr) {
            r.add(t);
        }
        return r;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        createContext();

        for (TypeElement annotation : annotations) {
            Set<? extends Element> elements = env.getElementsAnnotatedWith(annotation);
            Set<TypeElement> typeElements = ElementFilter.typesIn(elements);
            for (TypeElement e : typeElements) {

                DevkitTypeElement devkitTypeElement = new DevkitTypeElementImpl(e);
                try {
                    for (Plugin plugin : getPlugins()) {
                        for (Validator validator : plugin.getValidators()) {
                            if (validator.shouldValidate(processingEnv.getOptions())) {
                                validator.validate(devkitTypeElement, context);
                            }
                        }
                        for (Generator generator : plugin.getGenerators()) {
                            generator.generate(devkitTypeElement, context);
                        }
                    }
                } catch (ValidationException tve) {
                    error(tve.getMessage(), tve.getElement());
                    return false;
                } catch (GenerationException ge) {
                    error(ge.getMessage());
                    return false;
                }
            }
        }

        try {
            context.getCodeModel().build();
        } catch (IOException e) {
            error(e.getMessage());
            return false;
        }

        try {
            context.getSchemaModel().build();
        } catch (IOException e) {
            error(e.getMessage());
            return false;
        }

        try {
            context.getStudioModel().build();
        } catch (IOException e) {
            error(e.getMessage());
            return false;
        }

        return true;
    }

    private void createContext() {
        context = new GeneratorContext(processingEnv.getFiler(), processingEnv.getTypeUtils(), processingEnv.getElementUtils(), processingEnv.getOptions());
    }

    protected GeneratorContext getContext() {
        return context;
    }

    protected void note(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, msg);
    }

    protected void warn(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, msg);
    }

    protected void error(String msg) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg);
    }

    protected void error(String msg, Element element) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, msg, element);
    }
}
