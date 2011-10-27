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
import org.mule.api.Capabilities;
import org.mule.api.ConnectionManager;
import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.param.ConnectionKey;
import org.mule.api.lifecycle.Disposable;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.config.PoolingProfile;
import org.mule.devkit.generation.AbstractMessageGenerator;
import org.mule.devkit.generation.DevKitTypeElement;
import org.mule.devkit.generation.GenerationException;
import org.mule.devkit.model.code.Cast;
import org.mule.devkit.model.code.CatchBlock;
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
import org.mule.devkit.model.code.TryStatement;
import org.mule.devkit.model.code.Variable;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import java.util.Map;

public class ConnectionManagerGenerator extends AbstractMessageGenerator {

    @Override
    protected boolean shouldGenerate(DevKitTypeElement typeElement) {
        ExecutableElement connectMethod = connectMethodForClass(typeElement);
        ExecutableElement disconnectMethod = disconnectMethodForClass(typeElement);

        if (connectMethod == null || disconnectMethod == null) {
            return false;
        }

        return true;
    }

    @Override
    protected void doGenerate(DevKitTypeElement typeElement) throws GenerationException {
        ExecutableElement connectMethod = connectMethodForClass(typeElement);
        ExecutableElement disconnectMethod = disconnectMethodForClass(typeElement);
        ExecutableElement validateConnectionMethod = validateConnectionMethodForClass(typeElement);

        DefinedClass connectionManagerClass = getConnectionManagerAdapterClass(typeElement);

        // generate fields for each connection parameters
        Map<String, AbstractMessageGenerator.FieldVariableElement> fields = generateStandardFieldForEachParameter(connectionManagerClass, connectMethod);

        // generate fields for each configurable field
        for (VariableElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            FieldVariable configField = connectionManagerClass.field(Modifier.PRIVATE, ref(field.asType()), field.getSimpleName().toString());
            generateSetter(connectionManagerClass, configField);
            generateGetter(connectionManagerClass, configField);
        }
        
        // logger field
        FieldVariable logger = generateLoggerField(connectionManagerClass);        

        // standard fields
        FieldVariable muleContext = generateFieldForMuleContext(connectionManagerClass);
        FieldVariable flowConstruct = generateFieldForFlowConstruct(connectionManagerClass);

        // generate field for connection pool
        FieldVariable connectionPool = generateFieldForConnectionPool(connectionManagerClass);
        FieldVariable poolingProfile = connectionManagerClass.field(Modifier.PROTECTED, ref(PoolingProfile.class), "connectionPoolingProfile");

        // generate getter and setter for pooling profile
        generateSetter(connectionManagerClass, poolingProfile);
        generateGetter(connectionManagerClass, poolingProfile);

        // generate setters for all parameters
        for (String fieldName : fields.keySet()) {
            generateSetter(connectionManagerClass, fields.get(fieldName).getField());
            generateGetter(connectionManagerClass, fields.get(fieldName).getField());
        }

        // standard fields setters
        generateSetFlowConstructMethod(connectionManagerClass, flowConstruct);
        generateSetMuleContextMethod(connectionManagerClass, muleContext);

        DefinedClass connectionKeyClass = getConnectionParametersClass(typeElement, connectionManagerClass);

        // generate key fields
        Map<String, AbstractMessageGenerator.FieldVariableElement> keyFields = generateStandardFieldForEachParameter(connectionKeyClass, connectMethod);

        // generate constructor for key
        generateKeyConstructor(connectMethod, connectionKeyClass, keyFields);

        // generate setters for all keys
        for (String fieldName : keyFields.keySet()) {
            generateSetter(connectionKeyClass, keyFields.get(fieldName).getField());
            generateGetter(connectionKeyClass, keyFields.get(fieldName).getField());
        }

        generateConnectionKeyHashCodeMethod(connectMethod, connectionKeyClass);
        generateConnectionKeyEqualsMethod(connectMethod, connectionKeyClass);

        DefinedClass connectionFactoryClass = getConnectorFactoryClass(connectionManagerClass);

        FieldVariable connectionManagerInFactory = connectionFactoryClass.field(Modifier.PRIVATE,
                connectionManagerClass, "connectionManager");

        Method connectionFactoryConstructor = connectionFactoryClass.constructor(Modifier.PUBLIC);
        Variable constructorConnectionManager = connectionFactoryConstructor.param(connectionManagerClass, "connectionManager");
        connectionFactoryConstructor.body().assign(ExpressionFactory._this().ref(connectionManagerInFactory),
                constructorConnectionManager);

        generateMakeObjectMethod(typeElement, connectMethod, connectionFactoryClass, connectionKeyClass, connectionManagerInFactory);
        generateDestroyObjectMethod(connectMethod, disconnectMethod, connectionKeyClass, connectionFactoryClass);
        generateValidateObjectMethod(connectionFactoryClass, logger, validateConnectionMethod);
        generateActivateObjectMethod(connectionFactoryClass, validateConnectionMethod, connectMethod, keyFields, connectionKeyClass);
        generatePassivateObjectMethod(connectionFactoryClass);

        generateInitialiseMethod(connectionManagerClass, connectionPool, poolingProfile, connectionFactoryClass);

        generateBorrowConnectionMethod(connectMethod, connectionManagerClass, connectionPool, connectionKeyClass);
        generateReturnConnectionMethod(connectMethod, connectionManagerClass, connectionPool, connectionKeyClass);
        generateDestroyConnectionMethod(connectMethod, connectionManagerClass, connectionPool, connectionKeyClass);

        generateIsCapableOf(typeElement, connectionManagerClass);
    }

    private void generateConnectionKeyHashCodeMethod(ExecutableElement connect, DefinedClass connectionKeyClass) {
        Method hashCode = connectionKeyClass.method(Modifier.PUBLIC, context.getCodeModel().INT, "hashCode");
        Variable hash = hashCode.body().decl(context.getCodeModel().INT, "hash", ExpressionFactory.lit(1));

        for (VariableElement variable : connect.getParameters()) {
            if (variable.getAnnotation(ConnectionKey.class) == null) {
                continue;
            }

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

    private void generateConnectionKeyEqualsMethod(ExecutableElement connect, DefinedClass connectionKey) {
        Method equals = connectionKey.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "equals");
        Variable obj = equals.param(ref(Object.class), "obj");
        Expression areEqual = Op._instanceof(obj, connectionKey);

        for (VariableElement variable : connect.getParameters()) {
            if (variable.getAnnotation(ConnectionKey.class) == null) {
                continue;
            }

            String fieldName = variable.getSimpleName().toString();
            areEqual = Op.cand(areEqual, Op.eq(
                    ExpressionFactory._this().ref(fieldName),
                    ExpressionFactory.cast(connectionKey, obj).ref(fieldName)
            ));
        }

        equals.body()._return(
                areEqual
        );
    }

    private void generateBorrowConnectionMethod(ExecutableElement connect, DefinedClass connectionManagerClass, FieldVariable connectionPool, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) connect.getEnclosingElement()));
        Method borrowConnector = connectionManagerClass.method(Modifier.PUBLIC, connectorClass, "acquireConnection");
        Variable key = borrowConnector.param(connectionKeyClass, "key");
        borrowConnector._throws(ref(Exception.class));

        borrowConnector.body()._return(
                ExpressionFactory.cast(connectorClass,
                        connectionPool.invoke("borrowObject").arg(
                                key
                        ))
        );
    }

    private void generateReturnConnectionMethod(ExecutableElement connect, DefinedClass connectionManagerClass, FieldVariable connectionPool, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) connect.getEnclosingElement()));
        Method returnConnector = connectionManagerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "releaseConnection");
        Variable key = returnConnector.param(connectionKeyClass, "key");
        returnConnector._throws(ref(Exception.class));
        Variable connection = returnConnector.param(connectorClass, "connection");
        returnConnector.body().add(
                connectionPool.invoke("returnObject").arg(
                        key
                ).arg(connection)
        );
    }

    private void generateDestroyConnectionMethod(ExecutableElement connect, DefinedClass connectionManagerClass, FieldVariable connectionPool, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) connect.getEnclosingElement()));
        Method destroyConnector = connectionManagerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "destroyConnection");
        Variable key = destroyConnector.param(connectionKeyClass, "key");
        destroyConnector._throws(ref(Exception.class));
        Variable connection = destroyConnector.param(connectorClass, "connection");
        destroyConnector.body().add(
                connectionPool.invoke("invalidateObject").arg(
                        key
                ).arg(connection)
        );
    }

    private void generateInitialiseMethod(DefinedClass connectionManagerClass, FieldVariable connectionPool, FieldVariable connectionPoolingProfile, DefinedClass connectionFactoryClass) {
        Method initialisableMethod = connectionManagerClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "initialise");

        //initialisableMethod.body().add(ExpressionFactory.ref("super").invoke("initialise"));

        Variable config = initialisableMethod.body().decl(ref(GenericKeyedObjectPool.Config.class), "config",
                ExpressionFactory._new(ref(GenericKeyedObjectPool.Config.class)));

        Conditional ifNotNull = initialisableMethod.body()._if(Op.ne(connectionPoolingProfile, ExpressionFactory._null()));
        ifNotNull._then().assign(config.ref("maxIdle"), connectionPoolingProfile.invoke("getMaxIdle"));
        ifNotNull._then().assign(config.ref("maxActive"), connectionPoolingProfile.invoke("getMaxActive"));
        ifNotNull._then().assign(config.ref("maxWait"), connectionPoolingProfile.invoke("getMaxWait"));
        ifNotNull._then().assign(config.ref("whenExhaustedAction"), ExpressionFactory.cast(context.getCodeModel().BYTE, connectionPoolingProfile.invoke("getExhaustedAction")));

        Invocation newObjectFactory = ExpressionFactory._new(connectionFactoryClass);
        newObjectFactory.arg(ExpressionFactory._this());
        initialisableMethod.body().assign(connectionPool, ExpressionFactory._new(ref(GenericKeyedObjectPool.class)).arg(
                newObjectFactory
        ).arg(config));
    }

    private void generateActivateObjectMethod(DefinedClass connectionFactoryClass, ExecutableElement validateConnectionMethod, ExecutableElement connect, Map<String, FieldVariableElement> keyFields, DefinedClass connectionKeyClass) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) validateConnectionMethod.getEnclosingElement()));
        Method activateObject = connectionFactoryClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "activateObject");
        activateObject._throws(ref(Exception.class));
        Variable key = activateObject.param(Object.class, "key");
        Variable obj = activateObject.param(Object.class, "obj");

        Conditional ifNotKey = activateObject.body()._if(Op.not(Op._instanceof(key, connectionKeyClass)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Conditional ifNotObj = activateObject.body()._if(Op.not(Op._instanceof(obj, connectorClass)));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid connector type"));
        
        Cast casterConnector = ExpressionFactory.cast(connectorClass, obj);
        TryStatement tryDisconnect = activateObject.body()._try();
        Conditional ifNotConnected = tryDisconnect.body()._if(Op.not(casterConnector.invoke(validateConnectionMethod.getSimpleName().toString())));
        Cast castedConnectionKey = ExpressionFactory.cast(connectionKeyClass, key);
        Invocation connectInvoke = ExpressionFactory.cast(connectorClass, obj).invoke(connect.getSimpleName().toString());
        for (String fieldName : keyFields.keySet()) {
            connectInvoke.arg(castedConnectionKey.invoke("get" + StringUtils.capitalize(keyFields.get(fieldName).getField().name())));
        }
        ifNotConnected._then().add(connectInvoke);

        
        CatchBlock catchAndRethrow = tryDisconnect._catch(ref(Exception.class));
        Variable e = catchAndRethrow.param("e");
        catchAndRethrow.body()._throw(e);
        
    }

    private void generatePassivateObjectMethod(DefinedClass connectionFactoryClass) {
        Method passivateObject = connectionFactoryClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "passivateObject");
        passivateObject._throws(ref(Exception.class));
        passivateObject.param(Object.class, "key");
        passivateObject.param(Object.class, "obj");
    }

    private void generateValidateObjectMethod(DefinedClass connectionFactoryClass, FieldVariable logger, ExecutableElement validateConnectionMethod) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) validateConnectionMethod.getEnclosingElement()));
        Method validateObject = connectionFactoryClass.method(Modifier.PUBLIC, context.getCodeModel().BOOLEAN, "validateObject");
        validateObject.param(Object.class, "key");
        Variable obj = validateObject.param(Object.class, "obj");

        Conditional ifNotObj = validateObject.body()._if(Op.not(Op._instanceof(obj, connectorClass)));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid connector type"));

        Cast casterConnector = ExpressionFactory.cast(connectorClass, obj);
        TryStatement tryDisconnect = validateObject.body()._try();
        tryDisconnect.body()._return(casterConnector.invoke(validateConnectionMethod.getSimpleName().toString()));
        CatchBlock catchAndRethrow = tryDisconnect._catch(ref(Exception.class));
        Variable e = catchAndRethrow.param("e");
        catchAndRethrow.body().add(logger.invoke("error").arg(e.invoke("getMessage")).arg(e));
        catchAndRethrow.body()._return(ExpressionFactory.FALSE);
    }

    private void generateDestroyObjectMethod(ExecutableElement connect, ExecutableElement disconnect, DefinedClass connectionKeyClass, DefinedClass connectionFactoryClass) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) connect.getEnclosingElement()));

        Method destroyObject = connectionFactoryClass.method(Modifier.PUBLIC, context.getCodeModel().VOID, "destroyObject");
        destroyObject._throws(ref(Exception.class));
        Variable key = destroyObject.param(Object.class, "key");
        Variable obj = destroyObject.param(Object.class, "obj");
        Conditional ifNotKey = destroyObject.body()._if(Op.not(Op._instanceof(key, connectionKeyClass)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Conditional ifNotObj = destroyObject.body()._if(Op.not(Op._instanceof(obj, connectorClass)));
        ifNotObj._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid connector type"));

        Cast casterConnector = ExpressionFactory.cast(connectorClass, obj);
        TryStatement tryDisconnect = destroyObject.body()._try();
        tryDisconnect.body().add(casterConnector.invoke(disconnect.getSimpleName().toString()));
        CatchBlock catchAndRethrow = tryDisconnect._catch(ref(Exception.class));
        Variable e = catchAndRethrow.param("e");
        catchAndRethrow.body()._throw(e);
        tryDisconnect._finally()._if(Op._instanceof(casterConnector, ref(Stoppable.class)))._then().add(casterConnector.invoke("stop"));
        tryDisconnect._finally()._if(Op._instanceof(casterConnector, ref(Disposable.class)))._then().add(casterConnector.invoke("dispose"));
    }

    private void generateMakeObjectMethod(DevKitTypeElement typeElement, ExecutableElement connect, DefinedClass connectionFactoryClass, DefinedClass connectionKey, FieldVariable connectionManagerInFactory) {
        DefinedClass connectorClass = context.getClassForRole(context.getNameUtils().generateConnectorObjectRoleKey((TypeElement) connect.getEnclosingElement()));
        Method makeObject = connectionFactoryClass.method(Modifier.PUBLIC, Object.class, "makeObject");
        makeObject._throws(ref(Exception.class));
        Variable key = makeObject.param(Object.class, "key");
        Conditional ifNotKey = makeObject.body()._if(Op.not(Op._instanceof(key, connectionKey)));
        ifNotKey._then()._throw(ExpressionFactory._new(ref(RuntimeException.class)).arg("Invalid key type"));

        Variable connector = makeObject.body().decl(connectorClass, "connector", ExpressionFactory._new(connectorClass));

        for (VariableElement field : typeElement.getFieldsAnnotatedWith(Configurable.class)) {
            makeObject.body().add(connector.invoke("set" + StringUtils.capitalize(field.getSimpleName().toString()))
                    .arg(connectionManagerInFactory.invoke("get" + StringUtils.capitalize(field.getSimpleName().toString()))));
        }

        makeObject.body()._if(Op._instanceof(connector, ref(Initialisable.class)))._then().add(connector.invoke("initialise"));
        makeObject.body()._if(Op._instanceof(connector, ref(Startable.class)))._then().add(connector.invoke("start"));

        makeObject.body()._return(connector);
    }

    private void generateKeyConstructor(ExecutableElement connect, DefinedClass connectionKeyClass, Map<String, FieldVariableElement> keyFields) {
        Method keyConstructor = connectionKeyClass.constructor(Modifier.PUBLIC);
        for (VariableElement variable : connect.getParameters()) {
            String fieldName = variable.getSimpleName().toString();
            Variable parameter = keyConstructor.param(ref(variable.asType()), fieldName);
            keyConstructor.body().assign(ExpressionFactory._this().ref(keyFields.get(fieldName).getField()), parameter);
        }
    }

    private FieldVariable generateFieldForConnectionPool(DefinedClass connectionManagerClass) {
        FieldVariable connectionPool = connectionManagerClass.field(Modifier.PRIVATE, ref(GenericKeyedObjectPool.class), "connectionPool");
        connectionPool.javadoc().add("Connector Pool");

        return connectionPool;
    }

    private DefinedClass getConnectionManagerAdapterClass(TypeElement typeElement) {
        String connectionManagerName = context.getNameUtils().generateClassName(typeElement, ".config", "ConnectionManager");
        org.mule.devkit.model.code.Package pkg = context.getCodeModel()._package(context.getNameUtils().getPackageName(connectionManagerName));

        DefinedClass classToExtend = context.getClassForRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement));
        context.setClassRole(context.getNameUtils().generateConnectorObjectRoleKey(typeElement), classToExtend);

        DefinedClass connectionManagerClass = pkg._class(context.getNameUtils().getClassName(connectionManagerName));
        connectionManagerClass._implements(ref(Initialisable.class));
        connectionManagerClass._implements(ref(Capabilities.class));
        connectionManagerClass._implements(ref(ConnectionManager.class).narrow(getConnectionParametersClass(typeElement, connectionManagerClass)).narrow(classToExtend));

        context.setClassRole(context.getNameUtils().generateModuleObjectRoleKey(typeElement), connectionManagerClass);

        connectionManagerClass.javadoc().add("A {@code " + connectionManagerClass.name() + "} is a wrapper around ");
        connectionManagerClass.javadoc().add(ref(typeElement.asType()));
        connectionManagerClass.javadoc().add(" that adds connection management capabilities to the pojo.");

        return connectionManagerClass;
    }

    private DefinedClass getConnectionParametersClass(TypeElement typeElement, DefinedClass connectionManagerClass) {
        try {
            DefinedClass connectionKey = connectionManagerClass._class(Modifier.PUBLIC | Modifier.STATIC, "ConnectionParameters");
            context.setClassRole(context.getNameUtils().generateConnectionParametersRoleKey(typeElement), connectionKey);
            return connectionKey;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }

    private DefinedClass getConnectorFactoryClass(DefinedClass connectorManagerClass) {
        try {
            DefinedClass objectFactory = connectorManagerClass._class(Modifier.PRIVATE | Modifier.STATIC, "ConnectionFactory");
            objectFactory._implements(KeyedPoolableObjectFactory.class);
            return objectFactory;
        } catch (ClassAlreadyExistsException e) {
            return e.getExistingClass();
        }
    }
}