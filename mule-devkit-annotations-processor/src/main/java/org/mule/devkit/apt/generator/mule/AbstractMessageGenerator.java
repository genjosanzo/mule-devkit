package org.mule.devkit.apt.generator.mule;

import com.sun.codemodel.JBlock;
import com.sun.codemodel.JCatchBlock;
import com.sun.codemodel.JClass;
import com.sun.codemodel.JConditional;
import com.sun.codemodel.JDefinedClass;
import com.sun.codemodel.JExpr;
import com.sun.codemodel.JExpression;
import com.sun.codemodel.JFieldVar;
import com.sun.codemodel.JInvocation;
import com.sun.codemodel.JMethod;
import com.sun.codemodel.JMod;
import com.sun.codemodel.JOp;
import com.sun.codemodel.JTryBlock;
import com.sun.codemodel.JVar;
import org.apache.commons.lang.StringUtils;
import org.mule.api.MuleContext;
import org.mule.api.expression.ExpressionManager;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.api.transformer.DataType;
import org.mule.api.transformer.Transformer;
import org.mule.config.i18n.CoreMessages;
import org.mule.devkit.annotations.SourceCallback;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.AbstractCodeGenerator;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.util.TemplateParser;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractMessageGenerator extends AbstractCodeGenerator {
    private List<String> typeList;

    public AbstractMessageGenerator(AnnotationProcessorContext context) {
        super(context);


        buildTypeList();
    }

    protected boolean isTypeSupported(VariableElement variableElement) {
        return typeList.contains(variableElement.asType().toString());
    }

    protected void buildTypeList() {
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


    protected JFieldVar generateFieldForPatternInfo(JDefinedClass messageProcessorClass) {
        return messageProcessorClass.field(JMod.PRIVATE, ref(TemplateParser.PatternInfo.class), "patternInfo");
    }

    protected JFieldVar generateFieldForExpressionManager(JDefinedClass messageProcessorClass) {
        return messageProcessorClass.field(JMod.PRIVATE, ref(ExpressionManager.class), "expressionManager");
    }

    protected JFieldVar generateFieldForMuleContext(JDefinedClass messageProcessorClass) {
        return messageProcessorClass.field(JMod.PRIVATE, ref(MuleContext.class), "muleContext");
    }

    protected JFieldVar generateFieldForPojo(JDefinedClass messageProcessorClass, TypeElement typeElement) {
        return messageProcessorClass.field(JMod.PRIVATE, getLifecycleWrapperClass(typeElement), "object");
    }

    protected Map<String, FieldVariableElement> generateFieldForEachParameter(JDefinedClass messageProcessorClass, ExecutableElement executableElement) {
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = new HashMap<String, FieldVariableElement>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName()))
                continue;

            if (isTypeSupported(variable) || CodeModelUtils.isXmlType(variable)) {
                String fieldName = variable.getSimpleName().toString();
                JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(Object.class), fieldName);
                fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, variable));
            } else {
                String fieldName = variable.getSimpleName().toString();
                JFieldVar field = messageProcessorClass.field(JMod.PRIVATE, ref(variable.asType()), fieldName);
                fields.put(variable.getSimpleName().toString(), new AbstractMessageGenerator.FieldVariableElement(field, variable));
            }
        }
        return fields;
    }

    protected JMethod generateInitialiseMethod(JDefinedClass messageProcessorClass, JClass lifecycleWrapperClass, JFieldVar muleContext, JFieldVar expressionManager, JFieldVar patternInfo, JFieldVar object) {
        JMethod initialise = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "initialise");
        initialise._throws(InitialisationException.class);
        initialise.body().assign(expressionManager, muleContext.invoke("getExpressionManager"));
        initialise.body().assign(patternInfo, ref(TemplateParser.class).staticInvoke("createMuleStyleParser").invoke("getStyle"));

        JConditional ifNoObject = initialise.body()._if(JOp.eq(object, JExpr._null()));
        JTryBlock tryLookUp = ifNoObject._then()._try();
        tryLookUp.body().assign(object, muleContext.invoke("getRegistry").invoke("lookupObject").arg(JExpr.dotclass(lifecycleWrapperClass)));
        JCatchBlock catchBlock = tryLookUp._catch(ref(RegistrationException.class));
        JVar exception = catchBlock.param("e");
        JClass coreMessages = ref(CoreMessages.class);
        JInvocation failedToInvoke = coreMessages.staticInvoke("initialisationFailure");
        failedToInvoke.arg(lifecycleWrapperClass.fullName());
        JInvocation messageException = JExpr._new(ref(InitialisationException.class));
        messageException.arg(failedToInvoke);
        messageException.arg(exception);
        messageException.arg(JExpr._this());
        catchBlock.body()._throw(messageException);

        return initialise;
    }


    protected JMethod generateSetObjectMethod(JDefinedClass messageProcessorClass, JFieldVar object) {
        JMethod setObject = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setObject");
        JVar objectParam = setObject.param(object.type(), "object");
        setObject.body().assign(JExpr._this().ref(object), objectParam);

        return setObject;
    }


    protected JMethod generateSetMuleContextMethod(JDefinedClass messageProcessorClass, JFieldVar muleContext) {
        JMethod setMuleContext = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setMuleContext");
        JVar muleContextParam = setMuleContext.param(ref(MuleContext.class), "context");
        setMuleContext.body().assign(JExpr._this().ref(muleContext), muleContextParam);

        return setMuleContext;
    }

    protected void generateExpressionEvaluator(JBlock block, JVar evaluatedField, JFieldVar field, JFieldVar patternInfo, JFieldVar expressionManager, JVar muleMessage) {
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

    protected void generateTransform(JBlock block, JVar transformedField, JVar evaluatedField, TypeMirror expectedType, JFieldVar muleContext) {
        JInvocation isAssignableFrom = JExpr.dotclass(ref(expectedType).boxify()).invoke("isAssignableFrom").arg(evaluatedField.invoke("getClass"));
        JConditional ifIsAssignableFrom = block._if(JOp.not(isAssignableFrom));
        JBlock isAssignable = ifIsAssignableFrom._then();
        JVar dataTypeSource = isAssignable.decl(ref(DataType.class), "source");
        JVar dataTypeTarget = isAssignable.decl(ref(DataType.class), "target");

        isAssignable.assign(dataTypeSource, ref(DataTypeFactory.class).staticInvoke("create").arg(evaluatedField.invoke("getClass")));
        isAssignable.assign(dataTypeTarget, ref(DataTypeFactory.class).staticInvoke("create").arg(JExpr.dotclass(ref(expectedType).boxify())));

        JVar transformer = isAssignable.decl(ref(Transformer.class), "t");
        JInvocation lookupTransformer = muleContext.invoke("getRegistry").invoke("lookupTransformer");
        lookupTransformer.arg(dataTypeSource);
        lookupTransformer.arg(dataTypeTarget);
        isAssignable.assign(transformer, lookupTransformer);
        isAssignable.assign(transformedField, JExpr.cast(ref(expectedType).boxify(), transformer.invoke("transform").arg(evaluatedField)));

        JBlock notAssignable = ifIsAssignableFrom._else();
        notAssignable.assign(transformedField, JExpr.cast(ref(expectedType).boxify(), evaluatedField));
    }

    protected JMethod generateSetter(JDefinedClass messageProcessorClass, JFieldVar field) {
        JMethod setter = messageProcessorClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "set" + StringUtils.capitalize(field.name()));
        JVar value = setter.param(field.type(), "value");
        setter.body().assign(JExpr._this().ref(field), value);

        return setter;
    }


    protected void generateThrow(String bundle, Class<?> clazz, JCatchBlock callProcessorCatch, JExpression event, String methodName) {
        JVar exception = callProcessorCatch.param("e");
        JClass coreMessages = ref(CoreMessages.class);
        JInvocation failedToInvoke = coreMessages.staticInvoke(bundle);
        if (methodName != null) {
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

    protected class FieldVariableElement {
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


}
