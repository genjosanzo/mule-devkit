package org.mule.devkit.apt.generator.schema;

import com.sun.codemodel.JPackage;
import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Parameter;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.ContextualizedGenerator;
import org.mule.devkit.apt.generator.GenerationException;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/*
<?xml version="1.0" encoding="UTF-8" standalone="no"?>
<xsd:schema xmlns="${class.getNamespaceUri()}"
            xmlns:xsd="http://www.w3.org/2001/XMLSchema"
            xmlns:mule="http://www.mulesoft.org/schema/mule/core"
            xmlns:schemadoc="http://www.mulesoft.org/schema/mule/schemadoc"
            xmlns:beans="http://www.springframework.org/schema/beans"
            targetNamespace="${class.getNamespaceUri()}"
            elementFormDefault="qualified"
            attributeFormDefault="unqualified">

    <xsd:import namespace="http://www.w3.org/XML/1998/namespace"/>
    <xsd:import namespace="http://www.springframework.org/schema/beans"
                schemaLocation="http://www.springframework.org/schema/beans/spring-beans-3.0.xsd"/>
    <xsd:import namespace="http://www.mulesoft.org/schema/mule/core"
                schemaLocation="http://www.mulesoft.org/schema/mule/core/${class.getMuleVersion()}/mule.xsd"/>
    <xsd:import namespace="http://www.mulesoft.org/schema/mule/schemadoc"
                schemaLocation="http://www.mulesoft.org/schema/mule/schemadoc/${class.getMuleVersion()}/mule-schemadoc.xsd"/>

    <xsd:annotation>
        <xsd:documentation>
            This schema was auto-generated. Do not edit.
        </xsd:documentation>
    </xsd:annotation>

    <!-- Configurable -->
    <xsd:element name="config" type="configType" substitutionGroup="mule:abstract-extension"/>
    <xsd:complexType name="configType">
        <xsd:complexContent>
            <xsd:extension base="mule:abstractExtensionType">
                <xsd:attribute name="name" use="optional" type="xsd:string">
                    <xsd:annotation>
                        <xsd:documentation>
                            Give a name to this configuration so it can be later referenced by config-ref.
                        </xsd:documentation>
                    </xsd:annotation>
                </xsd:attribute>
            <#if class.getFactory()?has_content>
            <#list class.getFactory().getProperties() as property>
                <xsd:attribute name="<@uncapitalize>${property.getElementName()}</@uncapitalize>" type="${property.getType().getXmlType(true)}" use="<#if property.isOptional()>optional<#else>required</#if>"<#if property.hasDefaultValue()> default="${property.getDefaultValue()}"</#if>>
                    <#if property.getDescription()?has_content>
                    <xsd:annotation>
                        <xsd:documentation><![CDATA[
                            ${property.getDescription()}
                        ]]></xsd:documentation>
                    </xsd:annotation>
                    </#if>
                </xsd:attribute>
            </#list>
            <#else>
            <#list class.getProperties() as property>
                <#if property.isConfigurable()>
                <xsd:attribute name="<@uncapitalize>${property.getElementName()}</@uncapitalize>" type="${property.getType().getXmlType(true)}" use="<#if property.isOptional()>optional<#else>required</#if>"<#if property.hasDefaultValue()> default="${property.getDefaultValue()}"</#if>>
                    <#if property.getDescription()?has_content>
                    <xsd:annotation>
                        <xsd:documentation><![CDATA[
                            ${property.getDescription()}
                        ]]></xsd:documentation>
                    </xsd:annotation>
                    </#if>
                </xsd:attribute>
                </#if>
            </#list>
            </#if>
            </xsd:extension>
        </xsd:complexContent>
    </xsd:complexType>

    <!-- OAuth Operations -->
    <#if class.hasOAuth()>
    <xsd:element name="request-authorization" type="requestAuthorizationType" substitutionGroup="mule:abstract-message-processor">
        <xsd:annotation>
            <xsd:documentation><![CDATA[
                Request OAuth authorization. This operation will set http.status and Location properties to redirect the user to the
                authorization server.
            ]]></xsd:documentation>
        </xsd:annotation>
    </xsd:element>
    <xsd:complexType name="requestAuthorizationType">
        <xsd:complexContent>
            <xsd:extension base="mule:abstractInterceptingMessageProcessorType">
                <xsd:all>
                </xsd:all>
                <xsd:attribute name="config-ref" use="optional" type="xsd:string">
                    <xsd:annotation>
                        <xsd:documentation>
                            Specify which configuration to use for this invocation
                        </xsd:documentation>
                    </xsd:annotation>
                </xsd:attribute>
            </xsd:extension>
        </xsd:complexContent>
    </xsd:complexType>

    <xsd:element name="has-been-authorized" type="hasBeenAuthorizedType" substitutionGroup="mule:abstract-message-processor">
        <xsd:annotation>
            <xsd:documentation><![CDATA[
                Verifies if this connector has already been authorized. If it has not, then call request-authorization.
                This operation does not modifies the payload of the message, instead it adds an "authorized" outbound
                property.
            ]]></xsd:documentation>
        </xsd:annotation>
    </xsd:element>
    <xsd:complexType name="hasBeenAuthorizedType">
        <xsd:complexContent>
            <xsd:extension base="mule:abstractInterceptingMessageProcessorType">
                <xsd:all>
                </xsd:all>
                <xsd:attribute name="config-ref" use="optional" type="xsd:string">
                    <xsd:annotation>
                        <xsd:documentation>
                            Specify which configuration to use for this invocation
                        </xsd:documentation>
                    </xsd:annotation>
                </xsd:attribute>
            </xsd:extension>
        </xsd:complexContent>
    </xsd:complexType>

    <xsd:element name="request-access-token" type="requestAccessTokenType" substitutionGroup="mule:abstract-message-processor">
        <xsd:annotation>
            <xsd:documentation><![CDATA[
                Request an access token using the authorization code.
            ]]></xsd:documentation>
        </xsd:annotation>
    </xsd:element>
    <xsd:complexType name="requestAccessTokenType">
        <xsd:complexContent>
            <xsd:extension base="mule:abstractInterceptingMessageProcessorType">
                <xsd:all>
                </xsd:all>
                <xsd:attribute name="config-ref" use="optional" type="xsd:string">
                    <xsd:annotation>
                        <xsd:documentation>
                            Specify which configuration to use for this invocation
                        </xsd:documentation>
                    </xsd:annotation>
                </xsd:attribute>
            </xsd:extension>
        </xsd:complexContent>
    </xsd:complexType>

    <xsd:element name="set-authorization-code" type="setAuthorizationCodeType" substitutionGroup="mule:abstract-message-processor">
        <xsd:annotation>
            <xsd:documentation><![CDATA[
                Sets the autorization code received in the OAuth redirect URI.
            ]]></xsd:documentation>
        </xsd:annotation>
    </xsd:element>
    <xsd:complexType name="setAuthorizationCodeType">
        <xsd:complexContent>
            <xsd:extension base="mule:abstractInterceptingMessageProcessorType">
                <xsd:all>
                </xsd:all>

                <xsd:attribute name="config-ref" use="optional" type="xsd:string">
                    <xsd:annotation>
                        <xsd:documentation>
                            Specify which configuration to use for this invocation
                        </xsd:documentation>
                    </xsd:annotation>
                </xsd:attribute>
                <xsd:attribute name="code" type="xsd:string" use="required" >
                </xsd:attribute>
            </xsd:extension>
        </xsd:complexContent>
    </xsd:complexType>
    </#if>

    <!-- Operations -->
    <#list class.getMethods() as method>
    <#if method.isOperation()>
    <xsd:element name="<@splitCamelCase>${method.getElementName()}</@splitCamelCase>" type="${method.getElementName()}Type" substitutionGroup="mule:abstract-message-processor">
        <#if method.getDescription()?has_content>
        <xsd:annotation>
            <xsd:documentation><![CDATA[
                ${method.getDescription()}
            ]]></xsd:documentation>
        </xsd:annotation>
        </#if>
    </xsd:element>
    <xsd:complexType name="${method.getElementName()}Type">
        <xsd:complexContent>
            <xsd:extension base="mule:abstractInterceptingMessageProcessorType">
                <xsd:all>
                    <#list method.getParameters() as parameter>
                    <#if parameter.getType().isArray() || parameter.getType().isList()>
                    <xsd:element name="<@uncapitalize>${parameter.getElementName()}</@uncapitalize>"<#if parameter.isOptional()> minOccurs="0"</#if>>
                        <xsd:complexType>
                            <xsd:sequence>
                                <xsd:element name="<@singularize>${parameter.getElementName()}</@singularize>" minOccurs="0" maxOccurs="unbounded"
                                             <#if parameter.getType().isList()>
                                             type="${parameter.getType().getTypeArguments().get(0).getXmlType(false)}"/>
                                             <#else>
                                             type="${parameter.getType().getXmlType(false)}"/>
                                             </#if>
                            </xsd:sequence>
                        </xsd:complexType>
                    </xsd:element>
                    <#elseif parameter.getType().isMap()>
                    <xsd:element name="<@uncapitalize>${parameter.getElementName()}</@uncapitalize>" <#if parameter.isOptional()> minOccurs="0"</#if>>
                        <xsd:complexType>
                            <xsd:sequence>
                                <xsd:element name="<@singularize>${parameter.getElementName()}</@singularize>" minOccurs="0" maxOccurs="unbounded">
                                    <xsd:complexType>
                                        <xsd:attribute name="key" type="${parameter.getType().getTypeArguments().get(0).getXmlType(false)}"/>
                                        <xsd:attribute name="value" type="${parameter.getType().getTypeArguments().get(1).getXmlType(false)}"/>
                                    </xsd:complexType>
                                </xsd:element>
                            </xsd:sequence>
                        </xsd:complexType>
                    </xsd:element>
                    </#if>
                    </#list>
                </xsd:all>

                <xsd:attribute name="config-ref" use="optional" type="xsd:string">
                    <xsd:annotation>
                        <xsd:documentation>
                            Specify which configuration to use for this invocation
                        </xsd:documentation>
                    </xsd:annotation>
                </xsd:attribute>
                <#list method.getParameters() as parameter>
                <#if !parameter.getType().isArray() && !parameter.getType().isList() && !parameter.getType().isMap()>
                <xsd:attribute name="<@uncapitalize>${parameter.getElementName()}</@uncapitalize>" type="${parameter.getType().getXmlType(false)}" <#if !parameter.isOptional()>use="required" </#if><#if parameter.hasDefaultValue()>default="${parameter.getDefaultValue()}"</#if>>
                    <#if parameter.getDescription()?has_content>
                    <xsd:annotation>
                        <xsd:documentation><![CDATA[
                            ${parameter.getDescription()}
                        ]]></xsd:documentation>
                    </xsd:annotation>
                    </#if>
                </xsd:attribute>
                </#if>
                </#list>
            </xsd:extension>
        </xsd:complexContent>
    </xsd:complexType>

    </#if>
    </#list>

    <!-- Transformers -->
    <#list class.getMethods() as method>
    <#if method.isTransformer()>
    <xsd:element name="<@splitCamelCase>${method.getElementName()}</@splitCamelCase>" type="mule:abstractTransformerType"
                 substitutionGroup="mule:abstract-transformer">
        <#if method.getDescription()?has_content>
        <xsd:annotation>
            <xsd:documentation><![CDATA[
                ${method.getDescription()}
            ]]></xsd:documentation>
        </xsd:annotation>
        </#if>
    </xsd:element>
    </#if>
    </#list>

    <!-- Enums -->
    <#list class.getEnums() as enum>
    <xsd:simpleType name="${enum.getXmlType(false)}">
        <xsd:union>
            <xsd:simpleType>
                <xsd:restriction base="xsd:string">
                    <#list enum.getValues() as enumValue>
                    <xsd:enumeration value="${enumValue}" />
                    </#list>
                </xsd:restriction>
            </xsd:simpleType>
            <xsd:simpleType>
                <xsd:restriction base="xsd:string">
                    <xsd:pattern value="\#\[[^\]]+\]"/>
                </xsd:restriction>
            </xsd:simpleType>
        </xsd:union>
    </xsd:simpleType>

    </#list>

    <!-- XML Types -->
    <#list class.getXmlTypes() as xmlType>
    ${xmlType.getJavaClass().getXmlComplexType()}

    </#list>
</xsd:schema>

 */
public class SchemaGenerator extends ContextualizedGenerator {
    private static final String BASE_NAMESPACE = "http://www.mulesoft.org/schema/mule/";
    private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
    private static final String XSD_NAMESPACE = "http://www.w3.org/2001/XMLSchema";
    private static final String SPRING_FRAMEWORK_NAMESPACE = "http://www.springframework.org/schema/beans";
    private static final String SPRING_FRAMEWORK_SCHEMA_LOCATION = "http://www.springframework.org/schema/beans/spring-beans-3.0.xsd";
    private static final String MULE_NAMESPACE = "http://www.mulesoft.org/schema/mule/core";
    private static final String MULE_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/core/3.1/mule.xsd";
    private static final String MULE_SCHEMADOC_NAMESPACE = "http://www.mulesoft.org/schema/mule/schemadoc";
    private static final String MULE_SCHEMADOC_SCHEMA_LOCATION = "http://www.mulesoft.org/schema/mule/schemadoc/3.1/mule-schemadoc.xsd";
    private static final QName MULE_ABSTRACT_EXTENSION = new QName(MULE_NAMESPACE, "abstract-extension", "mule");
    private static final QName MULE_ABSTRACT_EXTENSION_TYPE = new QName(MULE_NAMESPACE, "abstractExtensionType", "mule");
    private static final QName MULE_ABSTRACT_MESSAGE_PROCESSOR = new QName(MULE_NAMESPACE, "abstract-message-processor", "mule");
    private static final QName MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE = new QName(MULE_NAMESPACE, "abstractInterceptingMessageProcessorType", "mule");
    private static final QName STRING = new QName(XSD_NAMESPACE, "string", "xs");
    private static final QName DECIMAL = new QName(XSD_NAMESPACE, "decimal", "xs");
    private static final QName FLOAT = new QName(XSD_NAMESPACE, "float", "xs");
    private static final QName INTEGER = new QName(XSD_NAMESPACE, "integer", "xs");
    private static final QName DOUBLE = new QName(XSD_NAMESPACE, "double", "xs");
    private static final QName DATETIME = new QName(XSD_NAMESPACE, "dateTime", "xs");
    private static final QName LONG = new QName(XSD_NAMESPACE, "long", "xs");
    private static final QName BYTE = new QName(XSD_NAMESPACE, "byte", "xs");
    private static final QName BOOLEAN = new QName(XSD_NAMESPACE, "boolean", "xs");
    private static final QName ANYURI = new QName(XSD_NAMESPACE, "anyURI", "xs");
    private static final String USE_REQUIRED = "required";
    private static final String USE_OPTIONAL = "optional";
    private static Map<String, QName> typeMap;
    private Schema schema;
    private ObjectFactory objectFactory;

    static {
        typeMap = new HashMap<String, QName>();
        typeMap.put("java.lang.String", new QName("xs:string"));
        typeMap.put("int", new QName("integerType"));
        typeMap.put("float", new QName("floatType"));
        typeMap.put("long", new QName("longType"));
        typeMap.put("byte", new QName("byteType"));
        typeMap.put("short", new QName("integerType"));
        typeMap.put("double", new QName("doubleType"));
        typeMap.put("boolean", new QName("booleanType"));
        typeMap.put("char", new QName("charType"));
        typeMap.put("java.lang.Integer", new QName("integerType"));
        typeMap.put("java.lang.Float", new QName("floatType"));
        typeMap.put("java.lang.Long", new QName("longType"));
        typeMap.put("java.lang.Byte", new QName("byteType"));
        typeMap.put("java.lang.Short", new QName("integerType"));
        typeMap.put("java.lang.Double", new QName("doubleType"));
        typeMap.put("java.lang.Boolean", new QName("booleanType"));
        typeMap.put("java.lang.Character", new QName("charType"));
        typeMap.put("java.util.Date", new QName("dateTimeType"));
        typeMap.put("java.net.URL", new QName("anyUriType"));
        typeMap.put("java.net.URI", new QName("anyUriType"));
    }

    public SchemaGenerator(AnnotationProcessorContext context) {
        super(context);

        schema = new Schema();
        objectFactory = new ObjectFactory();
    }

    public void generate(TypeElement type) throws GenerationException {

        Module module = type.getAnnotation(Module.class);
        schema.setTargetNamespace(BASE_NAMESPACE + module.name());
        schema.setElementFormDefault(FormChoice.QUALIFIED);
        schema.setAttributeFormDefault(FormChoice.QUALIFIED);

        importXmlNamespace();
        importSpringFrameworkNamespace();
        importMuleNamespace();
        importMuleSchemaDocNamespace();

        registerTypes();

        registerConfigElement(type);

        registerProcessors(type);

        try {
            OutputStream schemaStream = getContext().getCodeWriter().openBinary(null, "META-INF/mule-" + module.name() + ".xsd");

            FileTypeSchema fileTypeSchema = new FileTypeSchema(schemaStream, schema, type);
            getContext().addSchema(module, fileTypeSchema);
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }
    }

    private void registerProcessors(TypeElement type) {
        java.util.List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Processor processor = method.getAnnotation(Processor.class);
            if (processor == null)
                continue;

            String name = method.getSimpleName().toString();
            if (processor.name().length() > 0)
                name = processor.name();

            Element element = new TopLevelElement();
            element.setName(name);
            element.setType(new QName(method.getSimpleName().toString() + "Type"));
            element.setSubstitutionGroup(MULE_ABSTRACT_MESSAGE_PROCESSOR);

            ComplexType complexType = new TopLevelComplexType();
            complexType.setName(method.getSimpleName().toString() + "Type");

            ComplexContent complexContent = new ComplexContent();
            complexType.setComplexContent(complexContent);
            ExtensionType complexContentExtension = new ExtensionType();
            complexContentExtension.setBase(MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE);
            complexContent.setExtension(complexContentExtension);

            Attribute configRefAttr = createAttribute("name", true, STRING, "Give a name to this configuration so it can be later referenced by config-ref.");
            complexContentExtension.getAttributeOrAttributeGroup().add(configRefAttr);

            for (VariableElement variable : method.getParameters()) {
                complexContentExtension.getAttributeOrAttributeGroup().add(createParameterAttribute(variable));
            }

            schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
            schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);
        }
    }

    private void registerConfigElement(TypeElement type) {
        ExtensionType config = registerExtension("config");
        Attribute nameAttribute = createAttribute("name", true, STRING, "Give a name to this configuration so it can be later referenced by config-ref.");
        config.getAttributeOrAttributeGroup().add(nameAttribute);

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(type.getEnclosedElements());
        for (VariableElement variable : variables) {
            config.getAttributeOrAttributeGroup().add(createConfigurableAttribute(variable));
        }
    }

    private Attribute createConfigurableAttribute(VariableElement variable) {
        Configurable configurable = variable.getAnnotation(Configurable.class);
        if (configurable == null)
            return null;

        String name = variable.getSimpleName().toString();
        if (configurable.name().length() > 0)
            name = configurable.name();

        Attribute attribute = new Attribute();

        // set whenever or not is optional
        attribute.setUse(configurable.optional() ? USE_OPTIONAL : USE_REQUIRED);

        if (isTypeSupported(variable)) {
            attribute.setName(name);
            attribute.setType(typeMap.get(variable.asType().toString()));
        } else {
            // non-supported types will get "-ref" so beans can be injected
            attribute.setName(name + "-ref");
            attribute.setType(STRING);
        }

        // add default value
        if (configurable.defaultValue().length() > 0) {
            attribute.setDefault(configurable.defaultValue());
        }
        return attribute;
    }

    private Attribute createParameterAttribute(VariableElement variable) {
        Parameter parameter = variable.getAnnotation(Parameter.class);

        String name = variable.getSimpleName().toString();
        if (parameter != null && parameter.name().length() > 0)
            name = parameter.name();

        Attribute attribute = new Attribute();

        // set whenever or not is optional
        if (parameter != null) {
            attribute.setUse(parameter.optional() ? USE_OPTIONAL : USE_REQUIRED);
        } else {
            attribute.setUse(USE_REQUIRED);
        }


        if (isTypeSupported(variable)) {
            attribute.setName(name);
            attribute.setType(typeMap.get(variable.asType().toString()));
        } else {
            // non-supported types will get "-ref" so beans can be injected
            attribute.setName(name + "-ref");
            attribute.setType(STRING);
        }

        // add default value
        if (parameter != null && parameter.defaultValue().length() > 0) {
            attribute.setDefault(parameter.defaultValue());
        }
        return attribute;
    }


    private boolean isTypeSupported(VariableElement variableElement) {
        return typeMap.containsKey(variableElement.asType().toString());
    }

    private void importMuleSchemaDocNamespace() {
        Import muleSchemaDocImport = new Import();
        muleSchemaDocImport.setNamespace(MULE_SCHEMADOC_NAMESPACE);
        muleSchemaDocImport.setSchemaLocation(MULE_SCHEMADOC_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleSchemaDocImport);
    }

    private void importMuleNamespace() {
        Import muleSchemaImport = new Import();
        muleSchemaImport.setNamespace(MULE_NAMESPACE);
        muleSchemaImport.setSchemaLocation(MULE_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(muleSchemaImport);
    }

    private void importSpringFrameworkNamespace() {
        Import springFrameworkImport = new Import();
        springFrameworkImport.setNamespace(SPRING_FRAMEWORK_NAMESPACE);
        springFrameworkImport.setSchemaLocation(SPRING_FRAMEWORK_SCHEMA_LOCATION);
        schema.getIncludeOrImportOrRedefine().add(springFrameworkImport);
    }

    private void importXmlNamespace() {
        Import xmlImport = new Import();
        xmlImport.setNamespace(XML_NAMESPACE);
        schema.getIncludeOrImportOrRedefine().add(xmlImport);
    }

    private Attribute createAttribute(String name, boolean optional, QName type, String description) {
        Attribute attr = new Attribute();
        attr.setName(name);
        attr.setUse(optional ? USE_OPTIONAL : USE_REQUIRED);
        attr.setType(type);
        Annotation nameAnnotation = new Annotation();
        attr.setAnnotation(nameAnnotation);
        Documentation nameDocumentation = new Documentation();
        nameDocumentation.getContent().add(description);
        nameAnnotation.getAppinfoOrDocumentation().add(nameDocumentation);

        return attr;
    }

    private ExtensionType registerExtension(String name) {
        ComplexType complexType = new TopLevelComplexType();
        complexType.setName(name + "Type");

        Element extension = new TopLevelElement();
        extension.setName(name);
        extension.setType(new QName(complexType.getName()));
        extension.setSubstitutionGroup(MULE_ABSTRACT_EXTENSION);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(complexContentExtension);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);
        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);

        return complexContentExtension;
    }

    private void registerTypes() {
        registerType("integerType", INTEGER);
        registerType("decimalType", DECIMAL);
        registerType("floatType", FLOAT);
        registerType("doubleType", DOUBLE);
        registerType("dateTimeType", DATETIME);
        registerType("longType", LONG);
        registerType("byteType", BYTE);
        registerType("booleanType", BOOLEAN);
        registerType("anyUriType", ANYURI);
        registerType("charType", STRING, 1, 1);
    }

    private void registerType(String name, QName base) {
        registerType(name, base, -1, -1);
    }

    private void registerType(String name, QName base, int minlen, int maxlen) {
        SimpleType simpleType = new TopLevelSimpleType();
        simpleType.setName(name);
        Union union = new Union();
        simpleType.setUnion(union);

        union.getSimpleType().add(createSimpleType(base, minlen, maxlen));
        union.getSimpleType().add(createExpressionSimpleType());

        schema.getSimpleTypeOrComplexTypeOrGroup().add(simpleType);
    }

    private LocalSimpleType createSimpleType(QName base, int minlen, int maxlen) {
        LocalSimpleType simpleType = new LocalSimpleType();
        Restriction restriction = new Restriction();
        restriction.setBase(base);

        if (minlen != -1) {
            NumFacet minLenFacet = new NumFacet();
            minLenFacet.setValue(Integer.toString(minlen));
            JAXBElement<NumFacet> element = objectFactory.createMinLength(minLenFacet);
            restriction.getFacets().add(element);
        }

        if (maxlen != -1) {
            NumFacet maxLenFacet = new NumFacet();
            maxLenFacet.setValue(Integer.toString(maxlen));
            JAXBElement<NumFacet> element = objectFactory.createMaxLength(maxLenFacet);
            restriction.getFacets().add(element);
        }

        simpleType.setRestriction(restriction);

        return simpleType;
    }

    private LocalSimpleType createExpressionSimpleType() {
        LocalSimpleType expression = new LocalSimpleType();
        Restriction restriction = new Restriction();
        expression.setRestriction(restriction);
        restriction.setBase(STRING);
        Pattern pattern = new Pattern();
        pattern.setValue("\\#\\[[^\\]]+\\]");
        restriction.getFacets().add(pattern);

        return expression;
    }
}
