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
import org.mule.api.MuleMessage;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.Processor;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transport.NullPayload;
import org.mule.util.TemplateParser;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MessageProcessorGenerator extends AbstractCodeGenerator {
    public MessageProcessorGenerator(AnnotationProcessorContext context) {
        super(context);
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
            String fieldName = variable.getSimpleName().toString();
            JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(Object.class), fieldName);
            fields.put(variable.getSimpleName().toString(), new FieldVariableElement(field, variable));
        }

        // add standard fields
        JFieldVar object = messageProcessorClass.field(JMod.PRIVATE, ref(executableElement.getEnclosingElement().asType()), "object");
        JFieldVar muleContext = messageProcessorClass.field(JMod.PRIVATE, ref(MuleContext.class), "muleContext");
        JFieldVar expressionManager = messageProcessorClass.field(JMod.PRIVATE, ref(ExpressionManager.class), "expressionManager");
        JFieldVar patternInfo = messageProcessorClass.field(JMod.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");

        // add initialise
        generateInitialiseMethod(messageProcessorClass, muleContext, expressionManager, patternInfo);

        // add setmulecontext
        generateSetMuleContextMethod(messageProcessorClass, muleContext);

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
        defaultMuleMessage.arg(event);
        defaultMuleEvent.arg(defaultMuleMessage);

        return defaultMuleEvent;
    }

    private void generateProcessMethod(ExecutableElement executableElement, JDefinedClass messageProcessorClass, Map<String, FieldVariableElement> fields, JFieldVar muleContext, JFieldVar object, JFieldVar expressionManager, JFieldVar patternInfo) {
        String methodName = executableElement.getSimpleName().toString();
        JType muleEvent = ref(MuleEvent.class);

        JMethod process = messageProcessorClass.method(JMod.PUBLIC, muleEvent, "process");
        JVar event = process.param(muleEvent, "event");
        JVar muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        process.body().assign(muleMessage, event.invoke("getMessage"));

        JTryBlock callProcessor = process.body()._try();

        LinkedList<JVar> parameters = new LinkedList<JVar>();
        for (String fieldName : fields.keySet()) {
            JVar parameter = callProcessor.body().decl(ref(Object.class), "evaluated" + StringUtils.capitalize(fieldName));
            parameters.addFirst(parameter);
            generateExpressionEvaluator(callProcessor.body(), parameter, fields.get(fieldName).getField(), patternInfo, expressionManager, muleMessage);
            generateTransform(callProcessor.body(), parameter, fields.get(fieldName).getVariableElement().asType(), muleContext);
        }

        generateMethodCall(callProcessor.body(), object, methodName, parameters, muleContext, event);
        generateThrowFailedToInvoke(callProcessor._catch((JClass) ref(Exception.class)), event, methodName);
    }

    private JVar generateMethodCall(JBlock body, JFieldVar object, String methodName, List<JVar> parameters, JFieldVar muleContext, JVar event) {
        JVar resultPayload = body.decl(ref(Object.class), "resultPayload");
        JInvocation methodCall = object.invoke(methodName);
        for (JVar parameter : parameters) {
            methodCall.arg(parameter);
        }
        body.assign(resultPayload, methodCall);
        body._if(resultPayload.eq(JExpr._null()))._then()._return(generateNullPayload(muleContext, event));

        return resultPayload;
    }

    private void generateThrowFailedToInvoke(JCatchBlock callProcessorCatch, JVar event, String methodName) {
        JVar exception = callProcessorCatch.param("e");
        JClass coreMessages = ref(CoreMessages.class).boxify();
        JInvocation failedToInvoke = coreMessages.staticInvoke("failedToInvoke");
        failedToInvoke.arg(JExpr.lit(methodName));
        failedToInvoke.arg(event);
        failedToInvoke.arg(exception);
        callProcessorCatch.body()._throw(JExpr._new(ref(MessagingException.class)).arg(failedToInvoke));
    }

    private JMethod generateInitialiseMethod(JDefinedClass messageProcessorClass, JFieldVar muleContext, JFieldVar expressionManager, JFieldVar patternInfo) {
        JMethod initialise = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "initialise");
        initialise._throws(InitialisationException.class);
        initialise.body().assign(expressionManager, muleContext.invoke("getExpressionManager"));
        initialise.body().assign(patternInfo, ref(TemplateParser.class).boxify().staticInvoke("createMuleStyleParser").invoke("getStyle"));

        return initialise;
    }

    private JMethod generateSetMuleContextMethod(JDefinedClass messageProcessorClass, JFieldVar muleContext) {
        JMethod setMuleContext = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setMuleContext");
        JVar muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(muleContext, muleContextParam);

        return setMuleContext;
    }

    private JMethod generateSetter(JDefinedClass messageProcessorClass, JFieldVar field) {
        JMethod setter = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        JVar value = setter.param(ref(Object.class), "value");
        setter.body().assign(field, value);

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

    private void generateTransform(JBlock block, JVar evaluatedField, TypeMirror expectedType, JFieldVar muleContext) {
        JInvocation isAssignableFrom = JExpr.dotclass(ref(expectedType).boxify()).invoke("isAssignableFrom").arg(evaluatedField.invoke("getClass"));
        JBlock isAssignable = block._if(JOp.not(isAssignableFrom))._then();
        JVar dataTypeSource = isAssignable.decl(ref(DataType.class), "source");
        JVar dataTypeTarget = isAssignable.decl(ref(DataType.class), "target");

        isAssignable.assign(dataTypeSource, ref(DataTypeFactory.class).boxify().staticInvoke("create").arg(evaluatedField.invoke("getClass")));
        isAssignable.assign(dataTypeTarget, ref(DataTypeFactory.class).boxify().staticInvoke("create").arg(JExpr.dotclass(ref(expectedType).boxify())));

        JVar transformer = isAssignable.decl(ref(Transformer.class), "t");
        JInvocation lookupTransformer = muleContext.invoke("getRegistry").invoke("lookupTransformer");
        lookupTransformer.arg(dataTypeSource);
        lookupTransformer.arg(dataTypeTarget);
        isAssignable.assign(transformer, lookupTransformer);
        isAssignable.assign(evaluatedField, JExpr.cast(ref(expectedType), transformer.invoke("transform").arg(evaluatedField)));
    }

}
