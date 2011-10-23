package org.mule.devkit;

import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.MessageFactory;
import org.mule.config.spring.SpringRegistryLifecycleManager;
import org.mule.lifecycle.RegistryLifecycleManager;
import org.mule.registry.AbstractRegistry;
import org.mule.util.StringUtils;
import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.support.GenericApplicationContext;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DynamicSpringRegistry extends AbstractRegistry {
    public static final String REGISTRY_ID = "org.mule.devkit.Registry.Spring";
    public static final String SPRING_GENERIC_APPLICATION_CONTEXT = "springGenericApplicationContext";
    private GenericApplicationContext genericApplicationContext;

    public DynamicSpringRegistry(MuleContext muleContext) {
        super(REGISTRY_ID, muleContext);

        genericApplicationContext = new GenericApplicationContext();
    }

    @Override
    protected void doInitialise() throws InitialisationException {
    }

    @Override
    public void doDispose() {
        if (genericApplicationContext.isActive()) {
            genericApplicationContext.close();
        }

        genericApplicationContext = null;
    }

    @Override
    protected RegistryLifecycleManager createLifecycleManager() {
        return new SpringRegistryLifecycleManager(getRegistryId(), this, muleContext);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object lookupObject(String key) {
        if (StringUtils.isBlank(key)) {
            logger.warn(
                    MessageFactory.createStaticMessage("Detected a lookup attempt with an empty or null key"),
                    new Throwable().fillInStackTrace());
            return null;
        }

        if (key.equals(SPRING_GENERIC_APPLICATION_CONTEXT) && genericApplicationContext != null) {
            return genericApplicationContext;
        } else {
            try {
                return genericApplicationContext.getBean(key);
            } catch (NoSuchBeanDefinitionException e) {
                logger.debug(e);
                return null;
            }
        }
    }

    @Override
    public <T> Collection<T> lookupObjects(Class<T> type) {
        return lookupByType(type).values();
    }

    @Override
    public <T> Collection<T> lookupObjectsForLifecycle(Class<T> type) {
        return internalLookupByType(type, false, false).values();
    }

    @Override
    public <T> Map<String, T> lookupByType(Class<T> type) {
        return internalLookupByType(type, true, true);
    }

    protected <T> Map<String, T> internalLookupByType(Class<T> type, boolean nonSingletons, boolean eagerInit) {
        try {
            return genericApplicationContext.getBeansOfType(type, nonSingletons, eagerInit);
        } catch (FatalBeanException fbex) {
            String message = String.format("Failed to lookup beans of type %s from the dynamic spring registry", type);
            throw new MuleRuntimeException(MessageFactory.createStaticMessage(message), fbex);
        } catch (Exception e) {
            logger.debug(e);
            return Collections.emptyMap();
        }
    }

    @Override
    public void registerObject(String key, Object value) throws RegistrationException {
        if( !(value instanceof BeanDefinition) ) {
            throw new UnsupportedOperationException("Only bean definitions can be registered at this registry.");
        }
        this.genericApplicationContext.registerBeanDefinition(key, (BeanDefinition)value);
    }

    @Override
    public void registerObject(String key, Object value, Object metadata) throws RegistrationException {
        registerObject(key, value);
    }

    @Override
    public void registerObjects(Map<String, Object> objects) throws RegistrationException {
        for( String key : objects.keySet() ) {
            registerObject(key, objects.get(key));
        }
    }

    @Override
    public void unregisterObject(String key) {
        this.genericApplicationContext.removeBeanDefinition(key);
    }

    @Override
    public void unregisterObject(String key, Object metadata) throws RegistrationException {
        this.genericApplicationContext.removeBeanDefinition(key);
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @Override
    public boolean isRemote() {
        return false;
    }

}
