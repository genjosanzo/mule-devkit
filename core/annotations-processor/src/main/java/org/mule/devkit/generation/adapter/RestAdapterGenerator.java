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

package org.mule.devkit.generation.adapter;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpVersion;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.TraceMethod;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.oauth.OAuthAccessToken;
import org.mule.api.annotations.oauth.OAuthAccessTokenSecret;
import org.mule.api.annotations.rest.HttpMethod;
import org.mule.api.annotations.rest.RestCall;
import org.mule.api.annotations.rest.RestUriParam;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.devkit.generation.AbstractModuleGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.NamingContants;
import org.mule.devkit.model.code.Block;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.TypeReference;
import org.mule.devkit.model.code.Variable;
import org.mule.devkit.model.code.WhileLoop;
import sun.tools.tree.CatchStatement;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RestAdapterGenerator extends AbstractModuleGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        return typeElement.isModuleOrConnector() && typeElement.hasMethodsAnnotatedWith(RestCall.class);
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) {
        DefinedClass restClientAdapterClass = getRestClientAdapterClass(typeElement);

        // logger field
        FieldVariable logger = generateLoggerField(restClientAdapterClass);

        FieldVariable responseTimeout = restClientAdapterClass.field(Modifier.PRIVATE, context.getCodeModel().INT, "responseTimeout");
        FieldVariable httpClient = restClientAdapterClass.field(Modifier.PRIVATE | Modifier.VOLATILE, ref(HttpClient.class), "httpClient");
     
        Method initialise = restClientAdapterClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");
        initialise.annotate(ref(Override.class));
        initialise.body().add(ExpressionFactory._super().invoke("initialise"));
        initialise.body().assign(httpClient, ExpressionFactory._new(ref(HttpClient.class)));
        initialise.body().add(httpClient.invoke("getParams").invoke("setParameter").arg("http.protocol.version").arg(ref(HttpVersion.class).staticRef("HTTP_1_1")));
        initialise.body().add(httpClient.invoke("getParams").invoke("setParameter").arg("http.socket.timeout").arg(responseTimeout));
        initialise.body().add(httpClient.invoke("getParams").invoke("setParameter").arg("http.protocol.content-charset").arg("UTF-8"));

        generateSetter(restClientAdapterClass, responseTimeout);

        generateRestCallImplementations(typeElement, httpClient, restClientAdapterClass);
    }

    private void generateRestCallImplementations(DevKitTypeElement typeElement, FieldVariable httpClient, DefinedClass capabilitiesAdapter) {
        Map<String, Variable> variables = new HashMap<String, Variable>();
        for (ExecutableElement executableElement : typeElement.getMethodsAnnotatedWith(RestCall.class)) {
            Method override = capabilitiesAdapter.method(Modifier.PUBLIC, ref(executableElement.getReturnType()), executableElement.getSimpleName().toString());
            override._throws(ref(IOException.class));
            RestCall restCall = executableElement.getAnnotation(RestCall.class);

            for (VariableElement parameter : executableElement.getParameters()) {
                if (parameter.getAnnotation(OAuthAccessToken.class) != null ||
                        parameter.getAnnotation(OAuthAccessTokenSecret.class) != null) {
                    continue;
                }

                variables.put(
                        parameter.getSimpleName().toString(),
                        override.param(ref(parameter.asType()), parameter.getSimpleName().toString())
                );
            }

            Variable method = override.body().decl(ref(org.apache.commons.httpclient.HttpMethod.class), "method", ExpressionFactory._null());
            Variable queryString = override.body().decl(ref(List.class).narrow(ref(NameValuePair.class)), "queryString", ExpressionFactory._new(ref(ArrayList.class).narrow(ref(NameValuePair.class))));

            if (restCall.method() == HttpMethod.GET) {
                override.body().assign(method, ExpressionFactory._new(ref(GetMethod.class)));
            } else if (restCall.method() == HttpMethod.PUT) {
                override.body().assign(method, ExpressionFactory._new(ref(PutMethod.class)));
            } else if (restCall.method() == HttpMethod.DELETE) {
                override.body().assign(method, ExpressionFactory._new(ref(DeleteMethod.class)));
            } else if (restCall.method() == HttpMethod.POST) {
                override.body().assign(method, ExpressionFactory._new(ref(PostMethod.class)));
            } else if (restCall.method() == HttpMethod.TRACE) {
                override.body().assign(method, ExpressionFactory._new(ref(TraceMethod.class)));
            }

            override.body().add(method.invoke("setPath").arg(restCall.uri()));
            for (VariableElement parameter : executableElement.getParameters()) {
                addQueryParameter(override.body(), queryString, variables.get(parameter.getSimpleName().toString()), parameter);
            }

            for (VariableElement field : typeElement.getFieldsAnnotatedWith(RestUriParam.class)) {
                addQueryParameter(override.body(), queryString, ExpressionFactory.invoke("get" + StringUtils.capitalize(field.getSimpleName().toString())), field);
            }

            override.body().add(method.invoke("setQueryString").arg(queryString.invoke("toArray").arg(ExpressionFactory._new(ref(NameValuePair.class).array()))));

            override.body().add(httpClient.invoke("executeMethod").arg(method));

            Conditional ifMethodExecuted = override.body()._if(Op.cand(Op.ne(method, ExpressionFactory._null()), method.invoke("hasBeenUsed")));
            Variable bufferedReader = ifMethodExecuted._then().decl(ref(BufferedReader.class), "bufferedReader", ExpressionFactory._null());
            Variable stringWriter = ifMethodExecuted._then().decl(ref(StringWriter.class), "stringWriter", ExpressionFactory._new(ref(StringWriter.class)));
            Variable bufferedWriter = ifMethodExecuted._then().decl(ref(BufferedWriter.class), "bufferedWriter", ExpressionFactory._new(ref(BufferedWriter.class)).arg(stringWriter).arg(ExpressionFactory.lit(8192)));

            TryStatement readStream = ifMethodExecuted._then()._try();
            Variable line = readStream.body().decl(ref(String.class), "line", ExpressionFactory.lit(""));
            readStream.body().assign(bufferedReader, ExpressionFactory._new(ref(BufferedReader.class)).arg(ExpressionFactory._new(ref(InputStreamReader.class)).arg(method.invoke("getResponseBodyAsStream"))));
            WhileLoop whileLoop = readStream.body()._while(Op.ne(ExpressionFactory.assign(line, bufferedReader.invoke("readLine")), ExpressionFactory._null()));
            whileLoop.body().add(bufferedWriter.invoke("write").arg(line));
            whileLoop.body().add(bufferedWriter.invoke("newLine"));

            readStream._finally().add(bufferedWriter.invoke("flush"));
            readStream._finally().add(bufferedWriter.invoke("close"));
            readStream._finally()._if(Op.ne(bufferedReader, ExpressionFactory._null()))._then().add(bufferedReader.invoke("close"));

            ifMethodExecuted._then()._return(ref(StringEscapeUtils.class).staticInvoke("unescapeHtml").arg(stringWriter.invoke("toString")));

            override.body()._return(ExpressionFactory._null());
        }


    }

    private void addQueryParameter(Block body, Variable queryString, Expression variable, VariableElement parameter) {
        RestUriParam restUriParam = parameter.getAnnotation(RestUriParam.class);
        Expression rvalue = variable.invoke("toString");
        if (restUriParam != null) {
            if (restUriParam.separatedBy() != null &&
                !StringUtils.isEmpty(restUriParam.separatedBy())) {
                rvalue = ref(StringUtils.class).staticInvoke("join").arg(variable.invoke("toArray")).arg(restUriParam.separatedBy());
            }
            Conditional ifNotNull = body._if(Op.ne(variable, ExpressionFactory._null()));
            ifNotNull._then().add(queryString.invoke("add").arg(ExpressionFactory._new(ref(NameValuePair.class)).arg(restUriParam.value()).arg(rvalue)));
        }
    }

    private DefinedClass getRestClientAdapterClass(TypeElement typeElement) {
        String restClientAdapterClassName = context.getNameUtils().generateClassName(typeElement, NamingContants.ADAPTERS_NAMESPACE, NamingContants.REST_CLIENT_ADAPTER_CLASS_NAME_SUFFIX);
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(restClientAdapterClassName));
        TypeReference previous = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        if (previous == null) {
            previous = (TypeReference) ref(typeElement.asType());
        }

        DefinedClass clazz = pkg._class(context.getNameUtils().getClassName(restClientAdapterClassName), previous);
        clazz._implements(ref(Initialisable.class));
        clazz._implements(ref(Disposable.class));
        
        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), clazz);

        return clazz;
    }
}