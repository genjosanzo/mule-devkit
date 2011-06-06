package org.mule.devkit.apt.generator.schema;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.annotations.Module;
import org.mule.devkit.annotations.Parameter;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.annotations.SourceCallback;
import org.mule.devkit.annotations.Transformer;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.ContextualizedGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.mule.devkit.apt.util.Inflection;
import org.mule.devkit.apt.util.NameUtils;
import org.mule.util.StringUtils;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

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
    private static final QName MULE_ABSTRACT_TRANSFORMER = new QName(MULE_NAMESPACE, "abstract-transformer", "mule");
    private static final QName MULE_ABSTRACT_TRANSFORMER_TYPE = new QName(MULE_NAMESPACE, "abstractTransformerType", "mule");
    private static final QName MULE_ABSTRACT_INBOUND_ENDPOINT = new QName(MULE_NAMESPACE, "abstract-inbound-endpoint", "mule");
    private static final QName MULE_ABSTRACT_INBOUND_ENDPOINT_TYPE = new QName(MULE_NAMESPACE, "abstractInboundEndpointType", "mule");
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
    private Map<String, QName> typeMap;
    private Schema schema;
    private ObjectFactory objectFactory;

    public SchemaGenerator(AnnotationProcessorContext context) {
        super(context);

        schema = new Schema();
        objectFactory = new ObjectFactory();
        String[] pepe = new String[]{};
    }

    public void generate(TypeElement type) throws GenerationException {

        Module module = type.getAnnotation(Module.class);
        String targetNamespace = BASE_NAMESPACE + module.name();
        schema.setTargetNamespace(targetNamespace);
        schema.setElementFormDefault(FormChoice.QUALIFIED);
        schema.setAttributeFormDefault(FormChoice.UNQUALIFIED);

        buildTypeMap(targetNamespace);

        importXmlNamespace();
        importSpringFrameworkNamespace();
        importMuleNamespace();
        importMuleSchemaDocNamespace();

        registerTypes();
        registerConfigElement(type);
        registerProcessors(type);
        registerTransformers(type);

        try {
            OutputStream schemaStream = getContext().getCodeWriter().openBinary(null, "META-INF/mule-" + module.name() + ".xsd");

            FileTypeSchema fileTypeSchema = new FileTypeSchema(schemaStream, schema, type);
            getContext().addSchema(module, fileTypeSchema);
        } catch (IOException ioe) {
            throw new GenerationException(ioe);
        }
    }

    private void buildTypeMap(String targetNamespace) {
        typeMap = new HashMap<String, QName>();
        typeMap.put("java.lang.String", new QName(XSD_NAMESPACE, "string", "xs"));
        typeMap.put("int", new QName(targetNamespace, "integerType"));
        typeMap.put("float", new QName(targetNamespace, "floatType"));
        typeMap.put("long", new QName(targetNamespace, "longType"));
        typeMap.put("byte", new QName(targetNamespace, "byteType"));
        typeMap.put("short", new QName(targetNamespace, "integerType"));
        typeMap.put("double", new QName(targetNamespace, "doubleType"));
        typeMap.put("boolean", new QName(targetNamespace, "booleanType"));
        typeMap.put("char", new QName(targetNamespace, "charType"));
        typeMap.put("java.lang.Integer", new QName(targetNamespace, "integerType"));
        typeMap.put("java.lang.Float", new QName(targetNamespace, "floatType"));
        typeMap.put("java.lang.Long", new QName(targetNamespace, "longType"));
        typeMap.put("java.lang.Byte", new QName(targetNamespace, "byteType"));
        typeMap.put("java.lang.Short", new QName(targetNamespace, "integerType"));
        typeMap.put("java.lang.Double", new QName(targetNamespace, "doubleType"));
        typeMap.put("java.lang.Boolean", new QName(targetNamespace, "booleanType"));
        typeMap.put("java.lang.Character", new QName(targetNamespace, "charType"));
        typeMap.put("java.util.Date", new QName(targetNamespace, "dateTimeType"));
        typeMap.put("java.net.URL", new QName(targetNamespace, "anyUriType"));
        typeMap.put("java.net.URI", new QName(targetNamespace, "anyUriType"));
    }

    private void registerTransformers(TypeElement type) {
        java.util.List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Transformer transformer = method.getAnnotation(Transformer.class);
            if (transformer == null)
                continue;

            Element transformerElement = registerTransformer(NameUtils.uncamel(method.getSimpleName().toString()));
            schema.getSimpleTypeOrComplexTypeOrGroup().add(transformerElement);
        }
    }

    private void registerProcessors(TypeElement type) {


        Module module = type.getAnnotation(Module.class);
        String targetNamespace = BASE_NAMESPACE + module.name();

        java.util.List<ExecutableElement> methods = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement method : methods) {
            Processor processor = method.getAnnotation(Processor.class);
            if (processor == null)
                continue;

            String name = method.getSimpleName().toString();
            if (processor.name().length() > 0)
                name = processor.name();
            String typeName = StringUtils.capitalize(name) + "Type";

            registerProcessorElement(targetNamespace, name, typeName);

            registerProcessorType(targetNamespace, typeName, method);
        }

        for (ExecutableElement method : methods) {
            Source source = method.getAnnotation(Source.class);
            if (source == null)
                continue;

            String name = method.getSimpleName().toString();
            if (source.name().length() > 0)
                name = source.name();
            String typeName = StringUtils.capitalize(name) + "Type";

            registerSourceElement(targetNamespace, name, typeName);

            registerSourceType(targetNamespace, typeName, method);
        }

    }

    private void registerProcessorElement(String targetNamespace, String name, String typeName) {
        Element element = new TopLevelElement();
        element.setName(NameUtils.uncamel(name));
        element.setSubstitutionGroup(MULE_ABSTRACT_MESSAGE_PROCESSOR);
        element.setType(new QName(targetNamespace, typeName));

        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerSourceElement(String targetNamespace, String name, String typeName) {
        Element element = new TopLevelElement();
        element.setName(NameUtils.uncamel(name));
        element.setSubstitutionGroup(MULE_ABSTRACT_INBOUND_ENDPOINT);
        element.setType(new QName(targetNamespace, typeName));

        schema.getSimpleTypeOrComplexTypeOrGroup().add(element);
    }

    private void registerProcessorType(String targetNamespace, String name, ExecutableElement element) {
        registerExtendedType(MULE_ABSTRACT_MESSAGE_PROCESSOR_TYPE, targetNamespace, name, element);
    }

    private void registerSourceType(String targetNamespace, String name, ExecutableElement element) {
        registerExtendedType(MULE_ABSTRACT_INBOUND_ENDPOINT_TYPE, targetNamespace, name, element);
    }

    private void registerExtendedType(QName base, String targetNamespace, String name, ExecutableElement element) {
        TopLevelComplexType complexType = new TopLevelComplexType();
        complexType.setName(name);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(base);
        complexContent.setExtension(complexContentExtension);

        Attribute configRefAttr = createAttribute("config-ref", true, STRING, "Specify which configuration to use for this invocation.");
        complexContentExtension.getAttributeOrAttributeGroup().add(configRefAttr);

        All all = new All();
        complexContentExtension.setAll(all);

        if (element.getKind() == ElementKind.METHOD) {
            for (VariableElement variable : ((ExecutableElement) element).getParameters()) {
                if (variable.asType().toString().contains(SourceCallback.class.getName()))
                    continue;

                if (CodeModelUtils.isXmlType(variable)) {
                    all.getParticle().add(objectFactory.createElement(generateXmlElement(variable.getSimpleName().toString(), targetNamespace)));
                } else {
                    if (CodeModelUtils.isArrayOrList(getContext().getTypes(), variable.asType())) {
                        generateParameterCollectionElement(all, variable);
                    } else {
                        complexContentExtension.getAttributeOrAttributeGroup().add(createParameterAttribute(variable));
                    }
                }
            }
        }

        schema.getSimpleTypeOrComplexTypeOrGroup().add(complexType);
    }

    private void generateCollectionElement(All all, VariableElement variable, boolean optional) {
        TopLevelElement collectionElement = new TopLevelElement();
        all.getParticle().add(objectFactory.createElement(collectionElement));
        collectionElement.setName(variable.getSimpleName().toString());

        if (optional) {
            collectionElement.setMinOccurs(BigInteger.valueOf(0L));
        } else {
            collectionElement.setMinOccurs(BigInteger.valueOf(1L));
        }
        collectionElement.setMaxOccurs("1");

        LocalComplexType collectionComplexType = new LocalComplexType();
        collectionElement.setComplexType(collectionComplexType);
        ExplicitGroup sequence = new ExplicitGroup();
        collectionComplexType.setSequence(sequence);

        TopLevelElement collectionItemElement = new TopLevelElement();
        sequence.getParticle().add(objectFactory.createElement(collectionItemElement));

        collectionItemElement.setName(Inflection.singularize(collectionElement.getName()));
        collectionItemElement.setMinOccurs(BigInteger.valueOf(0L));
        collectionItemElement.setMaxOccurs("unbounded");

        DeclaredType variableType = (DeclaredType) variable.asType();
        java.util.List<? extends TypeMirror> variableTypeParameters = variableType.getTypeArguments();
        if (variableTypeParameters.size() != 0) {
            TypeMirror genericType = variableTypeParameters.get(0);

            if (isTypeSupported(genericType)) {
                collectionItemElement.setType(typeMap.get(genericType.toString()));
            } else {
                collectionItemElement.setComplexType(generateRefComplexType());
            }
        } else {
            collectionItemElement.setComplexType(generateRefComplexType());
        }
    }

    private void generateParameterCollectionElement(All all, VariableElement variable) {
        Parameter parameter = variable.getAnnotation(Parameter.class);
        boolean optional = true;

        if (parameter != null)
            optional = parameter.optional();

        generateCollectionElement(all, variable, optional);
    }

    private void generateConfigurableCollectionElement(All all, VariableElement variable) {
        Configurable configurable = variable.getAnnotation(Configurable.class);

        generateCollectionElement(all, variable, configurable.optional());
    }

    private LocalComplexType generateRefComplexType() {
        LocalComplexType itemComplexType = new LocalComplexType();

        Attribute refAttribute = new Attribute();
        refAttribute.setUse(USE_REQUIRED);
        refAttribute.setName("ref");
        refAttribute.setType(STRING);

        itemComplexType.getAttributeOrAttributeGroup().add(refAttribute);
        return itemComplexType;
    }

    private TopLevelElement generateXmlElement(String elementName, String targetNamespace) {
        TopLevelElement xmlElement = new TopLevelElement();
        xmlElement.setName(elementName);
        xmlElement.setType(new QName(targetNamespace, "XmlType"));
        return xmlElement;
    }

    private ComplexType createAnyXmlType() {
        ComplexType xmlComplexType = new TopLevelComplexType();
        xmlComplexType.setName("XmlType");
        Any any = new Any();
        any.setProcessContents("lax");
        any.setMinOccurs(new BigInteger("0"));
        any.setMaxOccurs("unbounded");
        ExplicitGroup all = new ExplicitGroup();
        all.getParticle().add(any);
        xmlComplexType.setSequence(all);

        Attribute ref = createAttribute("ref", true, STRING, "The reference object for this parameter");
        xmlComplexType.getAttributeOrAttributeGroup().add(ref);

        return xmlComplexType;
    }

    private void registerConfigElement(TypeElement type) {
        ExtensionType config = registerExtension("config");
        Attribute nameAttribute = createAttribute("name", true, STRING, "Give a name to this configuration so it can be later referenced by config-ref.");
        config.getAttributeOrAttributeGroup().add(nameAttribute);

        All all = new All();
        config.setAll(all);

        java.util.List<VariableElement> variables = ElementFilter.fieldsIn(type.getEnclosedElements());
        for (VariableElement variable : variables) {
            if (CodeModelUtils.isArrayOrList(getContext().getTypes(), variable.asType())) {
                generateConfigurableCollectionElement(all, variable);
            } else {
                config.getAttributeOrAttributeGroup().add(createConfigurableAttribute(variable));
            }
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

        if (isTypeSupported(variable.asType())) {
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


        if (isTypeSupported(variable.asType())) {
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

    private boolean isTypeSupported(TypeMirror typeMirror) {
        return typeMap.containsKey(typeMirror.toString());
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

    private Element registerTransformer(String name) {
        Element transformer = new TopLevelElement();
        transformer.setName(name);
        transformer.setSubstitutionGroup(MULE_ABSTRACT_TRANSFORMER);
        transformer.setType(MULE_ABSTRACT_TRANSFORMER_TYPE);

        return transformer;
    }

    private ExtensionType registerExtension(String name) {
        LocalComplexType complexType = new LocalComplexType();

        Element extension = new TopLevelElement();
        extension.setName(name);
        extension.setSubstitutionGroup(MULE_ABSTRACT_EXTENSION);
        extension.setComplexType(complexType);

        ComplexContent complexContent = new ComplexContent();
        complexType.setComplexContent(complexContent);
        ExtensionType complexContentExtension = new ExtensionType();
        complexContentExtension.setBase(MULE_ABSTRACT_EXTENSION_TYPE);
        complexContent.setExtension(complexContentExtension);

        schema.getSimpleTypeOrComplexTypeOrGroup().add(extension);

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

        registerAnyXmlType();
    }

    private void registerAnyXmlType() {
        ComplexType xmlComplexType = createAnyXmlType();
        schema.getSimpleTypeOrComplexTypeOrGroup().add(xmlComplexType);
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
