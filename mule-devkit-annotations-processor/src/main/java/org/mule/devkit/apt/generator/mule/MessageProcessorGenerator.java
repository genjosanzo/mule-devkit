package org.mule.devkit.apt.generator.mule;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JType;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.StringUtils;
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MessagingException;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.transformer.TransformerTemplate;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.TemplateParser;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractCodeGenerator {
    private List<String> typeList;

    public MessageProcessorGenerator(AnnotationProcessorContext context) {
        super(context);

        buildTypeList();
    }

    private class FieldVariableElement {
        private final JFieldVar field;
        private final VariableElement variableElement;

        public FieldVariableElement(JFieldVar field, VariableElement variableElement) {
            this.field = field;
            this.variableElement = variableElement;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((field == null) ? 0 : field.hashCode());
            result = prime * result + ((variableElement == null) ? 0 : variableElement.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FieldVariableElement other = (FieldVariableElement) obj;
            if (field == null) {
                if (other.field != null)
                    return false;
            } else if (!field.equals(other.field))
                return false;
            if (variableElement == null) {
                if (other.variableElement != null)
                    return false;
            } else if (!variableElement.equals(other.variableElement))
                return false;
            return true;
        }

        public JFieldVar getField() {
            return field;
        }

        public VariableElement getVariableElement() {
            return variableElement;
        }
    }

    public void generate(TypeElement type) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(type.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Processor processor = executableElement.getAnnotation(Processor.class);

            if (processor == null)
                continue;

            generateMessageProcessor(executableElement);
        }
    }

    private void generateMessageProcessor(ExecutableElement executableElement) {
        // get class
        JDefinedClass messageProcessorClass = getMessageProcessorClass(executableElement);

        // add a field for each argument of the method
        Map<String, FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (isTypeSupported(variable)) {
                String fieldName = variable.getSimpleName().toString();
                JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(Object.class), fieldName);
                fields.put(variable.getSimpleName().toString(), new FieldVariableElement(field, variable));
            } else {
                String fieldName = variable.getSimpleName().toString();
                JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(variable.asType()), fieldName);
                fields.put(variable.getSimpleName().toString(), new FieldVariableElement(field, variable));
            }
        }

        // add standard fields
        JFieldVar object = messageProcessorClass.field(JMod.PRIVATE, ref(executableElement.getEnclosingElement().asType()), "object");
        JFieldVar muleContext = messageProcessorClass.field(JMod.PRIVATE, ref(MuleContext.class), "muleContext");
        JFieldVar expressionManager = messageProcessorClass.field(JMod.PRIVATE, ref(ExpressionManager.class), "expressionManager");
        JFieldVar patternInfo = messageProcessorClass.field(JMod.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");

        // add initialise
        generateInitialiseMethod(messageProcessorClass, ref(executableElement.getEnclosingElement().asType()).boxify(), muleContext, expressionManager, patternInfo, object);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext);

        // add setobject
        generateSetObjectMethod(messageProcessorClass, object);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageProcessorClass, fields.get(fieldName).getField());
        }

        // add process method
        generateProcessMethod(executableElement, messageProcessorClass, fields, muleContext, object, expressionManager, patternInfo);
    }

    private JInvocation generateNullPayload(JFieldVar muleContext, JVar event) {
        JInvocation defaultMuleEvent = JExpr._new(ref(DefaultMuleEvent.class));
        JInvocation defaultMuleMessage = JExpr._new(ref(DefaultMuleMessage.class));
        JInvocation nullPayload = ref(NullPayload.class).boxify().staticInvoke("getInstance");
        defaultMuleMessage.arg(nullPayload);
        defaultMuleMessage.arg(muleContext);
        defaultMuleEvent.arg(defaultMuleMessage);
        defaultMuleEvent.arg(event);

        return defaultMuleEvent;
    }

    private void generateProcessMethod(ExecutableElement executableElement, JDefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, JFieldVar muleContext, JFieldVar object, JFieldVar expressionManager, JFieldVar patternInfo) {
        String methodName = executableElement.getSimpleName().toString();
        JType muleEvent = ref(MuleEvent.class);


        JMethod process = messageProcessorClass.method(JMod.PUBLIC, muleEvent, "process");
        process._throws(MuleException.class);
        JVar event = process.param(muleEvent, "event");
        JVar muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        process.body().assign(muleMessage, event.invoke("getMessage"));

        JTryBlock callProcessor = process.body()._try();

        LinkedList<JVar> parameters = new LinkedList<JVar>();
        for (String fieldName : fields.keySet()) {
            if (isTypeSupported(fields.get(fieldName).getVariableElement())) {
                JVar evaluated = callProcessor.body().decl(ref(Object.class), "evaluated" + StringUtils.capitalize(fieldName));
                JVar transformed = callProcessor.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName));
                generateExpressionEvaluator(callProcessor.body(), evaluated, fields.get(fieldName).getField(), patternInfo, expressionManager, muleMessage);
                generateTransform(callProcessor.body(), transformed, evaluated, fields.get(fieldName).getVariableElement().asType(), muleContext);
                parameters.addFirst(transformed);
            } else {
                //JVar ref = callProcessor.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "ref" + StringUtils.capitalize(fieldName));
                parameters.addFirst(fields.get(fieldName).getField());
            }
        }

        JType returnType = ref(executableElement.getReturnType());
        generateMethodCall(callProcessor.body(), object, methodName, parameters, muleContext, event, returnType);
        generateThrow("failedToInvoke", MessagingException.class, callProcessor._catch((JClass) ref(Exception.class)), event, methodName);
    }

    private JVar generateMethodCall(JBlock body, JFieldVar object, String methodName, List<JVar> parameters, JFieldVar muleContext, JVar event, JType returnType) {
        JVar resultPayload = null;
        if (returnType != getContext().getCodeModel().VOID) {
            resultPayload = body.decl(ref(Object.class), "resultPayload");
        }
        JInvocation methodCall = object.invoke(methodName);
        for (JVar parameter : parameters) {
            methodCall.arg(parameter);
        }

        if (returnType != getContext().getCodeModel().VOID) {
            body.assign(resultPayload, methodCall);
            body._if(resultPayload.eq(JExpr._null()))._then()._return(generateNullPayload(muleContext, event));
            generatePayloadOverwrite(body, event, resultPayload);
            body._return(event);
        } else {
            body.add(methodCall);
            body._return(event);
        }

        return resultPayload;
    }

    private void generatePayloadOverwrite(JBlock block, JVar event, JVar resultPayload) {
        JInvocation applyTransformers = event.invoke("getMessage").invoke("applyTransformers");
        applyTransformers.arg(event);
        JInvocation newTransformerTemplate = JExpr._new(ref(TransformerTemplate.class));
        JInvocation newOverwritePayloadCallback = JExpr._new(ref(TransformerTemplate.OverwitePayloadCallback.class));
        newOverwritePayloadCallback.arg(resultPayload);
        newTransformerTemplate.arg(newOverwritePayloadCallback);

        JVar transformerList = block.decl(ref(List.class).boxify().narrow(Transformer.class), "transformerList");
        block.assign(transformerList, JExpr._new(ref(ArrayList.class).boxify().narrow(Transformer.class)));
        block.add(transformerList.invoke("add").arg(newTransformerTemplate));

        applyTransformers.arg(transformerList);
        block.add(applyTransformers);
    }

    private void generateThrow(String bundle, Class<?> clazz, JCatchBlock callProcessorCatch, JVar event, String methodName) {
        JVar exception = callProcessorCatch.param("e");
        JClass coreMessages = ref(CoreMessages.class).boxify();
        JInvocation failedToInvoke = coreMessages.staticInvoke(bundle);
        if( methodName != null ) {
            failedToInvoke.arg(JExpr.lit(methodName));
        }
        JInvocation messageException = JExpr._new(ref(clazz));
        messageException.arg(failedToInvoke);
        if (event != null) {
            messageException.arg(event);
        }
        messageException.arg(exception);
        callProcessorCatch.body()._throw(messageException);
    }

    private JMethod generateInitialiseMethod(JDefinedClass messageProcessorClass, JClass messageProcessor, JFieldVar muleContext, JFieldVar expressionManager, JFieldVar patternInfo, JFieldVar object) {
        JMethod initialise = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "initialise");
        initialise._throws(InitialisationException.class);
        initialise.body().assign(expressionManager, muleContext.invoke("getExpressionManager"));
        initialise.body().assign(patternInfo, ref(TemplateParser.class).boxify().staticInvoke("createMuleStyleParser").invoke("getStyle"));

        JConditional ifNoObject = initialise.body()._if(JOp.eq(object, JExpr._null()));
        JTryBlock tryLookUp = ifNoObject._then()._try();
        tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(JExpr.dotclass(messageProcessor)));
        JCatchBlock catchBlock = tryLookUp._catch(ref(RegistrationException.class).boxify());
        JVar exception = catchBlock.param("e");
        JClass coreMessages = ref(CoreMessages.class).boxify();
        JInvocation failedToInvoke = coreMessages.staticInvoke("initialisationFailure");
        failedToInvoke.arg(messageProcessor.fullName());
        JInvocation messageException = JExpr._new(ref(InitialisationException.class));
        messageException.arg(failedToInvoke);
        messageException.arg(exception);
        messageException.arg(JExpr._this());
        catchBlock.body()._throw(messageException);


        /*

        try
        {
            object = muleContext.getRegistry().lookupObject(${class.getName()}.class);
        }
        catch (RegistrationException e)
        {
            throw new InitialisationException(
                CoreMessages.initialisationFailure(String.format(
                    "Multiple instances of '%s' were found in the registry so you need to configure a specific instance",
                    object.getClass())), this);
        }

         */

        return initialise;
    }

    private JMethod generateSetObjectMethod(JDefinedClass messageProcessorClass, JFieldVar object) {
        JMethod setObject = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setObject");
        JVar objectParam = setObject.param(object.type(), "object");
        setObject.body().assign(JExpr._this().ref(object), objectParam);

        return setObject;
    }


    private JMethod generateSetMuleContextMethod(JDefinedClass messageProcessorClass, JFieldVar muleContext) {
        JMethod setMuleContext = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setMuleContext");
        JVar muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(JExpr._this().ref(muleContext), muleContextParam);

        return setMuleContext;
    }

    private JMethod generateSetter(JDefinedClass messageProcessorClass, JFieldVar field) {
        JMethod setter = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        JVar value = setter.param(field.type(), "value");
        setter.body().assign(JExpr._this().ref(field), value);

        return setter;
    }

    private void generateExpressionEvaluator(JBlock block, JVar evaluatedField, JFieldVar field, JFieldVar patternInfo, JFieldVar expressionManager, JVar muleMessage) {
        JConditional conditional = block._if(JOp._instanceof(field, ref(String.class)));
        JBlock trueBlock = conditional._then();
        JConditional isPattern = trueBlock._if(JOp.cand(
                JExpr.invoke(JExpr.cast(ref(String.class), field), "startsWith").arg(patternInfo.invoke("getPrefix")),
                JExpr.invoke(JExpr.cast(ref(String.class), field), "endsWith").arg(patternInfo.invoke("getSuffix"))));
        JInvocation evaluate = expressionManager.invoke("evaluate");
        evaluate.arg(JExpr.cast(ref(String.class), field));
        evaluate.arg(muleMessage);
        isPattern._then().assign(evaluatedField, evaluate);
        JInvocation parse = expressionManager.invoke("parse");
        parse.arg(JExpr.cast(ref(String.class), field));
        parse.arg(muleMessage);
        isPattern._else().assign(evaluatedField, parse);
        JBlock falseBlock = conditional._else();
        falseBlock.assign(evaluatedField, field);
    }

    private void generateTransform(JBlock block, JVar transformedField, JVar evaluatedField, TypeMirror expectedType, JFieldVar muleContext) {
        JInvocation isAssignableFrom = JExpr.dotclass(ref(expectedType).boxify()).invoke("isAssignableFrom").arg(evaluatedField.invoke("getClass"));
        JConditional ifIsAssignableFrom = block._if(JOp.not(isAssignableFrom));
        JBlock isAssignable = ifIsAssignableFrom._then();
        JVar dataTypeSource = isAssignable.decl(ref(DataType.class), "source");
        JVar dataTypeTarget = isAssignable.decl(ref(DataType.class), "target");

        isAssignable.assign(dataTypeSource, ref(DataTypeFactory.class).boxify().staticInvoke("create").arg(evaluatedField.invoke("getClass")));
        isAssignable.assign(dataTypeTarget, ref(DataTypeFactory.class).boxify().staticInvoke("create").arg(JExpr.dotclass(ref(expectedType).boxify())));

        JVar transformer = isAssignable.decl(ref(Transformer.class), "t");
        JInvocation lookupTransformer = muleContext.invoke("getRegistry").invoke("lookupTransformer");
        lookupTransformer.arg(dataTypeSource);
        lookupTransformer.arg(dataTypeTarget);
        isAssignable.assign(transformer, lookupTransformer);
        isAssignable.assign(transformedField, JExpr.cast(ref(expectedType).boxify(), transformer.invoke("transform").arg(evaluatedField)));

        JBlock notAssignable = ifIsAssignableFrom._else();
        notAssignable.assign(transformedField, JExpr.cast(ref(expectedType).boxify(), evaluatedField));
    }

    private boolean isTypeSupported(VariableElement variableElement) {
        return typeList.contains(variableElement.asType().toString());
    }

    private void buildTypeList() {
        typeList = new ArrayList<String>();
        typeList.add("java.lang.String");
        typeList.add("int");
        typeList.add("float");
        typeList.add("long");
        typeList.add("byte");
        typeList.add("short");
        typeList.add("double");
        typeList.add("boolean");
        typeList.add("char");
        typeList.add("java.lang.Integer");
        typeList.add("java.lang.Float");
        typeList.add("java.lang.Long");
        typeList.add("java.lang.Byte");
        typeList.add("java.lang.Short");
        typeList.add("java.lang.Double");
        typeList.add("java.lang.Boolean");
        typeList.add("java.lang.Character");
        typeList.add("java.util.Date");
        typeList.add("java.net.URL");
        typeList.add("java.net.URI");
    }

}
