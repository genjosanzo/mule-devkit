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
package org.mule.devkit.model.meta;

import org.mule.api.Capabilities;
import org.mule.api.ConnectionException;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.registry.RegistrationException;
import org.mule.devkit.model.schema.Annotation;
import org.mule.devkit.model.schema.Documentation;
import org.mule.devkit.model.schema.Element;
import org.mule.devkit.model.schema.OpenAttrs;
import org.mule.devkit.model.schema.Schema;
import org.mule.devkit.model.schema.SchemaConstants;
import org.mule.devkit.model.schema.TopLevelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import java.io.InputStream;
import java.util.Map;

/**
 * Instances of MetaModule represents Mule modules in a running Mule application.
 */
public class MetaModule {
    private static Logger logger = LoggerFactory.getLogger(MetaModule.class);

    private MetaModel parentModel;
    private String name;
    private String namespace;
    private String description;
    private Class<?> _class;

    /**
     * Create a new module
     *
     * @param name        The name of the module
     * @param namespace   The namespace of the module
     * @param description Description of the module
     * @param _class      Class of the module
     */
    public MetaModule(MetaModel parentModel, String name, String namespace, String description, Class<?> _class) {
        this.parentModel = parentModel;
        this.name = name;
        this.namespace = namespace;
        this.description = description;
        this._class = _class;
    }

    /**
     * Parse the description of the config element in the schema
     *
     * @param schema Schema to be parsed
     * @return A string containing the description of the config element
     */
    private static String parseDescription(Schema schema) {
        for (OpenAttrs openAttr : schema.getSimpleTypeOrComplexTypeOrGroup()) {
            if (openAttr instanceof TopLevelElement) {
                Element element = (Element) openAttr;
                if (element.getName().equalsIgnoreCase(SchemaConstants.ELEMENT_NAME_CONFIG)) {
                    if (element.getComplexType() != null &&
                            element.getComplexType().getComplexContent() != null &&
                            element.getComplexType().getComplexContent().getExtension() != null &&
                            element.getComplexType().getComplexContent().getExtension().getAnnotation() != null) {
                        Annotation annotation = element.getComplexType().getComplexContent().getExtension().getAnnotation();
                        if (annotation.getAppinfoOrDocumentation() != null &&
                                annotation.getAppinfoOrDocumentation().size() > 0) {
                            if (annotation.getAppinfoOrDocumentation().get(0)
                                    instanceof Documentation) {
                                Documentation doc = (Documentation) annotation.getAppinfoOrDocumentation().get(0);
                                if (doc.getContent() != null && doc.getContent().size() > 0 &&
                                        doc.getContent().get(0) instanceof String) {
                                    return (String) doc.getContent().get(0);
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
    }


    /**
     * Parse the class of the config element in the schema
     *
     * @param schema Schema to be parsed
     * @return A string with the fully qualified name of the class
     */
    private static String parseClass(Schema schema) {
        for (OpenAttrs openAttr : schema.getSimpleTypeOrComplexTypeOrGroup()) {
            if (openAttr instanceof TopLevelElement) {
                Element element = (Element) openAttr;
                if (element.getName().equalsIgnoreCase(SchemaConstants.ELEMENT_NAME_CONFIG)) {
                    for (QName qName : element.getOtherAttributes().keySet()) {
                        if (qName.equals(SchemaConstants.MULE_DEVKIT_JAVA_CLASS_TYPE)) {
                            return element.getOtherAttributes().get(qName);
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Test whenever or not the connector can actually connect using defaults.
     *
     * @throws ConnectionException If a connection cannot be established
     */
    public void testConnectivity(String name) throws ConnectionException {
        testConnectivity(name, null);
    }

    /**
     * Test whenever or not the connector can actually connect using either the defaults
     * or a custom list of arguments.
     *
     * @param name                Name of the connector instance
     * @param connectionArguments A map representing connections arguments
     * @throws ConnectionException If a connection cannot be established
     */
    public void testConnectivity(String name, Map<String, Object> connectionArguments) throws ConnectionException {
        Capabilities connectorCapabilities = (Capabilities) parentModel.getMetaRegistry().lookupObject(name);
        if (connectorCapabilities == null) {
            throw new IllegalArgumentException("Cannot find a module named " + name);
        }

        // DO SOMETHING with the connector
    }

    public void newInstance(String name, Map<String, Object> configuration) throws InstantiationException {
        try {
            BeanDefinitionBuilder dynamicModuleBuilder = BeanDefinitionBuilder.rootBeanDefinition(_class);
            if (Initialisable.class.isAssignableFrom(_class)) {
                dynamicModuleBuilder.setInitMethodName(Initialisable.PHASE_NAME);
            }
            if (Disposable.class.isAssignableFrom(_class)) {
                dynamicModuleBuilder.setDestroyMethodName(Disposable.PHASE_NAME);
            }
            for (String attributeName : configuration.keySet()) {
                dynamicModuleBuilder.addPropertyValue(attributeName, configuration.get(attributeName));
            }

            parentModel.getMetaRegistry().registerObject(name, dynamicModuleBuilder.getBeanDefinition());
        } catch (RegistrationException e) {
            throw new InstantiationException(namespace, "Unable to create module", e);
        }
    }

    public static MetaModule parseSchema(MetaModel model, InputStream stream) {
        MetaModule metaModule = null;
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(Schema.class);
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Schema schema = (Schema) unmarshaller.unmarshal(stream);

            String name = schema.getTargetNamespace().substring(
                    schema.getTargetNamespace().lastIndexOf("/") + 1);
            String description = parseDescription(schema);
            String _class = parseClass(schema);
            if (_class == null) {
                return null;
            }
            metaModule = new MetaModule(model, name, schema.getTargetNamespace(), description, Class.forName(_class));
        } catch (JAXBException e) {
            logger.error(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.error(e.getMessage(), e);
        }

        return metaModule;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Class<?> getModuleClass() {
        return _class;
    }

    public void getModuleClass(Class<?> _class) {
        this._class = _class;
    }
}
