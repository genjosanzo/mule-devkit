package org.mule.devkit.apt.generator.mule;

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
import org.mule.DefaultMuleEvent;
import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.MuleSession;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.processor.MessageProcessor;
import org.mule.devkit.annotations.Source;
import org.mule.devkit.annotations.SourceCallback;
import org.mule.devkit.apt.AnnotationProcessorContext;
import org.mule.devkit.apt.generator.GenerationException;
import org.mule.devkit.apt.util.CodeModelUtils;
import org.mule.session.DefaultMuleSession;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MessageSourceGenerator extends AbstractMessageGenerator {


    public MessageSourceGenerator(AnnotationProcessorContext context) {
        super(context);
    }

    public void generate(TypeElement typeElement) throws GenerationException {
        List<ExecutableElement> executableElements = ElementFilter.methodsIn(typeElement.getEnclosedElements());
        for (ExecutableElement executableElement : executableElements) {
            Source source = executableElement.getAnnotation(Source.class);

            if (source == null)
                continue;

            generateMessageSource(typeElement, executableElement);
        }
    }

    private void generateMessageSource(TypeElement typeElement, ExecutableElement executableElement) {
        // get class
        JDefinedClass messageSourceClass = getMessageSourceClass(executableElement);

        // add a field for each argument of the method
        Map<String, FieldVariableElement> fields = generateFieldForEachParameter(messageSourceClass, executableElement);

        // add standard fields
        JFieldVar object = generateFieldForPojo(messageSourceClass, typeElement);
        JFieldVar muleContext = generateFieldForMuleContext(messageSourceClass);
        JFieldVar flowConstruct = messageSourceClass.field(JMod.PRIVATE, ref(FlowConstruct.class), "flowConstruct");
        JFieldVar messageProcessor = messageSourceClass.field(JMod.PRIVATE, ref(MessageProcessor.class), "messageProcessor");
        JFieldVar thread = messageSourceClass.field(JMod.PRIVATE, ref(Thread.class), "thread");

        // add initialise
        generateInitialiseMethod(messageSourceClass, getLifecycleWrapperClass(typeElement), muleContext, null, null, object);

        // add setmulecontext
        generateSetMuleContextMethod(messageSourceClass, muleContext);

        // add setobject
        generateSetObjectMethod(messageSourceClass, object);

        // add setlistener
        generateSetListenerMethod(messageSourceClass, messageProcessor);

        // add setflowconstruct
        generateSetFlowConstructMethod(messageSourceClass, flowConstruct);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(messageSourceClass, fields.get(fieldName).getField());
        }

        // add process method
        generateSourceCallbackMethod(messageSourceClass, executableElement, messageProcessor, muleContext, flowConstruct);

        // add start method
        generateStartMethod(messageSourceClass, thread);

        // add stop method
        generateStopMethod(messageSourceClass, thread);

        // add run method
        generateRunMethod(messageSourceClass, executableElement, fields, object, muleContext);
    }

    private void generateRunMethod(JDefinedClass messageSourceClass, ExecutableElement executableElement, Map<String, FieldVariableElement> fields, JFieldVar object, JFieldVar muleContext) {
        JMethod run = messageSourceClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "run");
        JTryBlock callSource = run.body()._try();

        String methodName = executableElement.getSimpleName().toString();

        List<JExpression> parameters = new ArrayList<JExpression>();
        for (VariableElement variable : executableElement.getParameters()) {
            if (variable.asType().toString().contains(SourceCallback.class.getName())) {
                parameters.add(JExpr._this());
            } else {
                String fieldName = variable.getSimpleName().toString();
                if (isTypeSupported(fields.get(fieldName).getVariableElement()) || CodeModelUtils.isXmlType(fields.get(fieldName).getVariableElement())) {
                    JVar transformed = callSource.body().decl(ref(fields.get(fieldName).getVariableElement().asType()).boxify(), "transformed" + StringUtils.capitalize(fieldName), JExpr._null());
                    JConditional notNull = callSource.body()._if(JOp.ne(fields.get(fieldName).getField(), JExpr._null()));
                    generateTransform(notNull._then(), transformed, fields.get(fieldName).getField(), fields.get(fieldName).getVariableElement().asType(), muleContext);
                    parameters.add(transformed);
                } else {
                    parameters.add(fields.get(fieldName).getField());
                }
            }
        }

        JInvocation methodCall = object.invoke(methodName);
        for (int i = 0; i < parameters.size(); i++) {
            methodCall.arg(parameters.get(i));
        }

        callSource.body().add(methodCall);

        //generateThrow("failedToInvoke", MessagingException.class, callSource._catch((JClass) ref(Exception.class)), JExpr.cast(ref(MuleEvent.class).boxify(), JExpr._null()), methodName);

        JCatchBlock swallowCatch = callSource._catch((JClass) ref(Exception.class));
        //JVar exception = swallowCatch.param("e");
        //swallowCatch.body()._throw(messageException);
    }


    private void generateStartMethod(JDefinedClass messageSourceClass, JFieldVar thread) {
        JMethod start = messageSourceClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "start");
        start._throws(ref(MuleException.class));
        JConditional ifNoThread = start.body()._if(JOp.eq(thread, JExpr._null()));
        JInvocation newThread = JExpr._new(ref(Thread.class));
        newThread.arg(JExpr._this());
        newThread.arg("Receiving Thread");
        ifNoThread._then().assign(thread, newThread);

        start.body().add(thread.invoke("start"));
    }

    private void generateStopMethod(JDefinedClass messageSourceClass, JFieldVar thread) {
        JMethod stop = messageSourceClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "stop");
        stop._throws(ref(MuleException.class));

        stop.body().add(thread.invoke("interrupt"));
    }


    private void generateSourceCallbackMethod(JDefinedClass messageSourceClass, ExecutableElement executableElement, JFieldVar messageProcessor, JFieldVar muleContext, JFieldVar flowConstruct) {
        JMethod process = messageSourceClass.method(JMod.PUBLIC, ref(Object.class), "process");
        JVar message = process.param(ref(Object.class), "message");

        JVar dummyImmutableEndpoint = process.body().decl(getDummyInboundEndpointClass(executableElement), "dummyImmutableEndpoint");
        JInvocation newDummyImmutableEndpoint = JExpr._new(getDummyInboundEndpointClass(executableElement));
        newDummyImmutableEndpoint.arg(muleContext);
        process.body().assign(dummyImmutableEndpoint, newDummyImmutableEndpoint);

        JVar muleMessage = process.body().decl(ref(MuleMessage.class), "muleMessage");
        JInvocation newMuleMessage = JExpr._new(ref(DefaultMuleMessage.class));
        newMuleMessage.arg(message);
        newMuleMessage.arg(muleContext);
        process.body().assign(muleMessage, newMuleMessage);

        JVar muleSession = process.body().decl(ref(MuleSession.class), "muleSession");
        JInvocation newMuleSession = JExpr._new(ref(DefaultMuleSession.class));
        newMuleSession.arg(flowConstruct);
        newMuleSession.arg(muleContext);
        process.body().assign(muleSession, newMuleSession);

        JVar muleEvent = process.body().decl(ref(MuleEvent.class), "muleEvent");
        JInvocation newMuleEvent = JExpr._new(ref(DefaultMuleEvent.class));
        newMuleEvent.arg(muleMessage);
        newMuleEvent.arg(dummyImmutableEndpoint);
        newMuleEvent.arg(muleSession);
        process.body().assign(muleEvent, newMuleEvent);

        JTryBlock tryBlock = process.body()._try();
        JVar responseEvent = tryBlock.body().decl(ref(MuleEvent.class), "responseEvent");
        JInvocation messageProcess = messageProcessor.invoke("process");
        messageProcess.arg(muleEvent);
        tryBlock.body().assign(responseEvent, messageProcess);
        JConditional ifResponse = tryBlock.body()._if(
                JOp.cand(JOp.ne(responseEvent, JExpr._null()),
                        JOp.ne(responseEvent.invoke("getMessage"), JExpr._null()))
        );
        ifResponse._then()._return(responseEvent.invoke("getMessage").invoke("getPayload"));

        JCatchBlock muleExceptionBlock = tryBlock._catch(ref(MuleException.class));
        process.body()._return(JExpr._null());
    }

    private JMethod generateSetListenerMethod(JDefinedClass messageSourceClass, JFieldVar messageProcessor) {
        JMethod setListener = messageSourceClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setListener");
        JVar listener = setListener.param(ref(MessageProcessor.class), "listener");
        setListener.body().assign(JExpr._this().ref(messageProcessor), listener);

        return setListener;
    }

    private JMethod generateSetFlowConstructMethod(JDefinedClass messageSourceClass, JFieldVar flowConstruct) {
        JMethod setFlowConstruct = messageSourceClass.method(JMod.PUBLIC, getContext().getCodeModel().VOID, "setFlowConstruct");
        JVar newFlowConstruct = setFlowConstruct.param(ref(FlowConstruct.class), "flowConstruct");
        setFlowConstruct.body().assign(JExpr._this().ref(flowConstruct), newFlowConstruct);

        return setFlowConstruct;
    }

}
