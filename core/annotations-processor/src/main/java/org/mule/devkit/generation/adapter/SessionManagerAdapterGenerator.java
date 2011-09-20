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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.mule.api.adapter.SessionManagerAdapter;
import org.mule.api.annotations.param.SessionKey;
import org.mule.api.lifecycle.Initialisable;
import org.mule.config.PoolingProfile;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevkitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.ClassAlreadyExistsException;
import org.mule.devkit.model.code.Conditional;
import org.mule.devkit.model.code.DefinedClass;
import org.mule.devkit.model.code.Expression;
import org.mule.devkit.model.code.ExpressionFactory;
import org.mule.devkit.model.code.FieldVariable;
import org.mule.devkit.model.code.Invocation;
import org.mule.devkit.model.code.Method;
import org.mule.devkit.model.code.Modifier;
import org.mule.devkit.model.code.Op;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Map;

public class SessionManagerAdapterGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevkitTypeElement typeElement) {
        ExecutableElement sessionCreate = createSessionForClass(typeElement);
        ExecutableElement sessionDestroy = destroySessionForClass(typeElement);

        if (sessionCreate == null || sessionDestroy == null)
            return false;

        return true;
    }

    @Override
    protected void doGenerate(DevkitTypeElement typeElement) throws GenerationException {
        ExecutableElement sessionCreate = createSessionForClass(typeElement);
        ExecutableElement sessionDestroy = destroySessionForClass(typeElement);

        DefinedClass sessionAdapter = getSessionAdapterClass(typeElement, sessionCreate.getReturnType());

        // generate fields for each session
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateStandardFieldForEachParameter(sessionAdapter, sessionCreate);

        // generate field for session pool
        FieldVariable sessionPool = generateFieldForSessionPool(sessionAdapter);
        FieldVariable poolingProfile = sessionAdapter.field(Modifier.PROTECTED, ref(PoolingProfile.class), "sessionPoolingProfile");

        // generate getter and setter for pooling profile
        generateSetter(sessionAdapter, poolingProfile);
        generateGetter(sessionAdapter, poolingProfile);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(sessionAdapter, fields.get(fieldName).getField());
            generateGetter(sessionAdapter, fields.get(fieldName).getField());
        }

        DefinedClass sessionAdapterKey = getSessionAdapterKeyClass(typeElement, sessionAdapter);

        // generate key fields
        Map<String, AbstractMessageGenerator.FieldVariableElement> keyFields = generateStandardFieldForEachParameter(sessionAdapterKey, sessionCreate);

        // generate constructor for key
        generateKeyConstructor(sessionCreate, sessionAdapterKey, keyFields);

        // generate setters for all keys
        for (String fieldName : keyFields.keySet()) {
            generateSetter(sessionAdapterKey, keyFields.get(fieldName).getField());
            generateGetter(sessionAdapterKey, keyFields.get(fieldName).getField());
        }

        generateSessionKeyHashCodeMethod(sessionCreate, sessionAdapterKey);
        generateSessionKeyEqualsMethod(sessionCreate, sessionAdapterKey);

        DefinedClass sessionAdapterObjectFactory = getSessionAdapterFactoryClass(sessionAdapter);

        // generate field for session adapter
        FieldVariable sessionAdapterField = generateFieldForSessionAdapter(sessionAdapterObjectFactory, sessionAdapter);

        generateObjectFactoryConstructor(sessionAdapterObjectFactory, sessionAdapterField);
        generateMakeObjectMethod(sessionCreate, sessionAdapterObjectFactory, sessionAdapterField, sessionAdapterKey, keyFields);
        generateDestroyObjectMethod(sessionCreate, sessionDestroy, sessionAdapterKey, sessionAdapterObjectFactory, sessionAdapterField);
        generateValidateObjectMethod(sessionAdapterObjectFactory);
        generateActivateObjectMethod(sessionAdapterObjectFactory);
        generatePassivateObjectMethod(sessionAdapterObjectFactory);

        generateInitialiseMethod(sessionAdapter, sessionPool, poolingProfile, sessionAdapterObjectFactory);

        generateBorrowSessionMethod(sessionCreate, sessionAdapter, sessionPool, sessionAdapterKey);
        generateReturnSessionMethod(sessionCreate, sessionAdapter, sessionPool, sessionAdapterKey);
        generateDestroySessionMethod(sessionCreate, sessionAdapter, sessionPool, sessionAdapterKey);
    }

    private void generateSessionKeyHashCodeMethod(ExecutableElement sessionCreate, DefinedClass sessionAdapterKey) {
        Method hashCode = sessionAdapterKey.method(Modifier.PUBLIC, context.getCodeModel().INT, "hashCode");
        Variable hash = hashCode.body().decl(context.getCodeModel().INT, "hash", ExpressionFactory.lit(1));

        for (VariableElement variable : sessionCreate.getParameters()) {
            if (variable.getAnnotation(SessionKey.class) == null)
                continue;

            String fieldName = variable.getSimpleName().toString();

            hashCode.body().assign(hash,
                    Op.plus(
                            Op.mul(hash, ExpressionFactory.lit(31)),
                            ExpressionFactory._this().ref(fieldName).invoke("hashCode")
                    )
            );
        }

        hashCode.body()._return(
                hash
        );
    }

    private void generateSessionKeyEqualsMethod(ExecutableElement sessionCreate, DefinedClass sessionAdapterKey) {
        Method equals = sessionAdapterKey.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "equals");
        Variable obj = equals.param(ref(Object.class), "obj");
        Expression areEqual = Op._instanceof(obj, sessionAdapterKey);

        for (VariableElement variable : sessionCreate.getParameters()) {
            if (variable.getAnnotation(SessionKey.class) == null)
                continue;

            String fieldName = variable.getSimpleName().toString();
            areEqual = Op.cand(areEqual, Op.eq(
                    ExpressionFactory._this().ref(fieldName),
                    ExpressionFactory.cast(sessionAdapterKey, obj).ref(fieldName)
            ));
        }

        equals.body()._return(
                areEqual
        );
    }

    private void generateBorrowSessionMethod(ExecutableElement sessionCreate, DefinedClass sessionAdapter, FieldVariable sessionPool, DefinedClass sessionAdapterKey) {
        Method borrowSession = sessionAdapter.method(Modifier.PUBLIC, ref(sessionCreate.getReturnType()), "borrowSession");
        Variable key = borrowSession.param(sessionAdapterKey, "key");
        borrowSession._throws(ref(Exception.class));
        /*
        Invocation newKey = ExpressionFactory._new(sessionAdapterKey);
        for (VariableElement variable : sessionCreate.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            Variable parameter = borrowSession.param(ref(variable.asType()), fieldName);
            newKey.arg(parameter);
        } */
        borrowSession.body()._return(
                ExpressionFactory.cast(ref(sessionCreate.getReturnType()),
                        sessionPool.invoke("borrowObject").arg(
                                key
                        ))
        );
    }

    private void generateReturnSessionMethod(ExecutableElement sessionCreate, DefinedClass sessionAdapter, FieldVariable sessionPool, DefinedClass sessionAdapterKey) {
        Method returnSession = sessionAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "returnSession");
        Variable key = returnSession.param(sessionAdapterKey, "key");
        returnSession._throws(ref(Exception.class));
        /*
        Invocation newKey = ExpressionFactory._new(sessionAdapterKey);
        for (VariableElement variable : sessionCreate.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            Variable parameter = returnSession.param(ref(variable.asType()), fieldName);
            newKey.arg(parameter);
        }
        */
        Variable session = returnSession.param(ref(sessionCreate.getReturnType()), "session");
        returnSession.body().add(
                sessionPool.invoke("returnObject").arg(
                        key
                ).arg(session)
        );
    }

    private void generateDestroySessionMethod(ExecutableElement sessionCreate, DefinedClass sessionAdapter, FieldVariable sessionPool, DefinedClass sessionAdapterKey) {
        Method destroySessin = sessionAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "destroySession");
        Variable key = destroySessin.param(sessionAdapterKey, "key");
        destroySessin._throws(ref(Exception.class));
        /*
        Invocation newKey = ExpressionFactory._new(sessionAdapterKey);
        for (VariableElement variable : sessionCreate.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            Variable parameter = destroySessin.param(ref(variable.asType()), fieldName);
            newKey.arg(parameter);
        }
        */
        Variable session = destroySessin.param(ref(sessionCreate.getReturnType()), "session");
        destroySessin.body().add(
                sessionPool.invoke("invalidateObject").arg(
                        key
                ).arg(session)
        );
    }

    private void generateInitialiseMethod(DefinedClass sessionAdapter, FieldVariable sessionPool, FieldVariable sessionPoolingProfile, DefinedClass sessionAdapterObjectFactory) {
        Method initialisableMethod = sessionAdapter.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");

        initialisableMethod.body().add(ExpressionFactory.ref("super").invoke("initialise"));

        Variable config = initialisableMethod.body().decl(ref(GenericKeyedObjectPool.Config.class), "config",
                ExpressionFactory._new(ref(GenericKeyedObjectPool.Config.class)));

        Conditional ifNotNull = initialisableMethod.body()._if(Op.ne(sessionPoolingProfile, ExpressionFactory._null()));
        ifNotNull._then().assign(config.ref("maxIdle"), sessionPoolingProfile.invoke("getMaxIdle"));
        ifNotNull._then().assign(config.ref("maxActive"), sessionPoolingProfile.invoke("getMaxActive"));
        ifNotNull._then().assign(config.ref("maxWait"), sessionPoolingProfile.invoke("getMaxWait"));
        ifNotNull._then().assign(config.ref("whenExhaustedAction"), ExpressionFactory.cast(context.getCodeModel().BYTE, sessionPoolingProfile.invoke("getExhaustedAction")));

        Invocation newObjectFactory = ExpressionFactory._new(sessionAdapterObjectFactory).arg(ExpressionFactory._this());
        initialisableMethod.body().assign(sessionPool, ExpressionFactory._new(ref(GenericKeyedObjectPool.class)).arg(
                newObjectFactory
        ).arg(config));
    }

    private void generateObjectFactoryConstructor(DefinedClass sessionAdapterObjectFactory, FieldVariable sessionAdapterField) {
        Method constructor = sessionAdapterObjectFactory.constructor(Modifier.PUBLIC);
        Variable sessionAdapater = constructor.param(sessionAdapterField.type(), sessionAdapterField.name());
        constructor.body().assign(ExpressionFactory._this().ref(sessionAdapterField), sessionAdapater);
    }

    private void generateActivateObjectMethod(DefinedClass sessionAdapterObjectFactory) {
        Method activateObject = sessionAdapterObjectFactory.method(Modifier.PUBLIC, context.getCodeModel().VOID, "activateObject");
        activateObject._throws(ref(Exception.class));
        Variable key = activateObject.param(Object.class, "key");
        Variable obj = activateObject.param(Object.class, "obj");
    }

    private void generatePassivateObjectMethod(DefinedClass sessionAdapterObjectFactory) {
        Method passivateObject = sessionAdapterObjectFactory.method(Modifier.PUBLIC, context.getCodeModel().VOID, "passivateObject");
        passivateObject._throws(ref(Exception.class));
        Variable key = passivateObject.param(Object.class, "key");
        Variable obj = passivateObject.param(Object.class, "obj");
    }

    private void generateValidateObjectMethod(DefinedClass sessionAdapterObjectFactory) {
        Method validateObject = sessionAdapterObjectFactory.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "validateObject");
        Variable key = validateObject.param(Object.class, "key");
        Variable obj = validateObject.param(Object.class, "obj");

        validateObject.body()._return(ExpressionFactory.TRUE);
    }

    private void generateDestroyObjectMethod(ExecutableElement sessionCreate, ExecutableElement sessionDestroy, DefinedClass sessionAdapterKey, DefinedClass sessionAdapterObjectFactory, FieldVariable sessionAdapterField) {
        Method destroyObject = sessionAdapterObjectFactory.method(Modifier.PUBLIC, context.getCodeModel().VOID, "destroyObject");
        destroyObject._throws(ref(Exception.class));
        Variable key = destroyObject.param(Object.class, "key");
        Variable obj = destroyObject.param(Object.class, "obj");
        Conditional ifNotKey = destroyObject.body()._if(Op.not(Op._instanceof(key, sessionAdapterKey)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Conditional ifNotObj = destroyObject.body()._if(Op.not(Op._instanceof(obj, ref(sessionCreate.getReturnType()))));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid session type"));

        Invocation destroySession = sessionAdapterField.invoke(sessionDestroy.getSimpleName().toString());
        destroySession.arg(ExpressionFactory.cast(ref(sessionCreate.getReturnType()), obj));

        destroyObject.body().add(destroySession);
    }

    private void generateMakeObjectMethod(ExecutableElement sessionCreate, DefinedClass sessionAdapterObjectFactory, FieldVariable sessionAdapter, DefinedClass sessionAdapterKey, Map<String, FieldVariableElement> keyFields) {
        Method makeObject = sessionAdapterObjectFactory.method(Modifier.PUBLIC, Object.class, "makeObject");
        makeObject._throws(ref(Exception.class));
        Variable key = makeObject.param(Object.class, "key");
        Conditional ifNotKey = makeObject.body()._if(Op.not(Op._instanceof(key, sessionAdapterKey)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Invocation createSession = sessionAdapter.invoke(sessionCreate.getSimpleName().toString());
        Cast sessionKey = ExpressionFactory.cast(sessionAdapterKey, key);
        for (String fieldName : keyFields.keySet()) {
            createSession.arg(sessionKey.invoke("get" + StringUtils.capitalize(keyFields.get(fieldName).getField().name())));
        }

        makeObject.body()._return(createSession);
    }

    private void generateKeyConstructor(ExecutableElement sessionCreate, DefinedClass sessionAdapterKey, Map<String, FieldVariableElement> keyFields) {
        Method keyConstructor = sessionAdapterKey.constructor(Modifier.PUBLIC);
        for (VariableElement variable : sessionCreate.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            Variable parameter = keyConstructor.param(ref(variable.asType()), fieldName);
            keyConstructor.body().assign(ExpressionFactory._this().ref(keyFields.get(fieldName).getField()), parameter);
        }
    }

    private FieldVariable generateFieldForSessionPool(DefinedClass sessionAdapter) {
        FieldVariable sessionPool = sessionAdapter.field(Modifier.PRIVATE, ref(GenericKeyedObjectPool.class), "sessionPool");
        sessionPool.javadoc().add("Session Pool");

        return sessionPool;
    }

    private FieldVariable generateFieldForSessionAdapter(DefinedClass sessionAdapterFactory, DefinedClass sessionAdapter) {
        FieldVariable sessionAdapterField = sessionAdapterFactory.field(Modifier.PRIVATE, sessionAdapter, "sessionAdapter");
        sessionAdapterField.javadoc().add("Session Adapter");

        return sessionAdapterField;
    }

    private DefinedClass getSessionAdapterClass(TypeElement typeElement, TypeMirror session) {
        String sessionAdapterName = context.getNameUtils().generateClassName(typeElement, ".config", "SessionManagerAdapter");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(sessionAdapterName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));

        DefinedClass sessionAdapter = pkg._class(context.getNameUtils().getClassName(sessionAdapterName), classToExtend);
        sessionAdapter._implements(ref(Initialisable.class));
        sessionAdapter._implements(ref(SessionManagerAdapter.class).narrow(getSessionAdapterKeyClass(typeElement, sessionAdapter)).narrow(ref(session)));

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), sessionAdapter);

        sessionAdapter.javadoc().add("A {@code " + sessionAdapter.name() + "} is a wrapper around ");
        sessionAdapter.javadoc().add(ref(typeElement.asType()));
        sessionAdapter.javadoc().add(" that adds session management to the pojo.");

        return sessionAdapter;
    }

    private DefinedClass getSessionAdapterKeyClass(TypeElement typeElement, DefinedClass sessionAdapter) {
        try {
            DefinedClass sessionKey = sessionAdapter._class(Modifier.PUBLIC | Modifier.STATIC, "SessionKey");
            context.setClassRole(context.getNameUtils().generateSessionKeyRoleKey(typeElement), sessionKey);
            return sessionKey;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    private DefinedClass getSessionAdapterFactoryClass(DefinedClass sessionAdapter) {
        try {
            DefinedClass objectFactory = sessionAdapter._class(Modifier.PRIVATE | Modifier.STATIC, "SessionFactory");
            objectFactory._implements(KeyedPoolableObjectFactory.class);
            return objectFactory;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }
}