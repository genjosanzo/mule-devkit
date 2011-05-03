package org.mule.devkit;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;
import org.mule.api.lifecycle.Initialisable;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.registry.RegistrationException;
import org.mule.config.i18n.CoreMessages;

import java.lang.reflect.ParameterizedType;

public abstract class SelfRegisteringMessageProcessor<Module> implements MessageProcessor, Initialisable, MuleContextAware {
    protected Module module;
    protected Class<Module> moduleClass;
    protected MuleContext muleContext;

    public void initialise() throws InitialisationException {
        moduleClass = (Class<Module>) ((ParameterizedType) (getClass().getGenericSuperclass())).getActualTypeArguments()[0];

        if (module == null) {
            lookupModuleInRegistry();
        }
    }

    protected void lookupModuleInRegistry() throws InitialisationException {
        try {
            module = muleContext.getRegistry().lookupObject(moduleClass);
        } catch (RegistrationException e) {
            throw new InitialisationException(
                    CoreMessages.initialisationFailure(String.format(
                            "Multiple instances of '%s' were found in the registry so you need to configure a specific instance",
                            moduleClass.getClass())), this);
        }

        if (module == null) {
            try {
                module = moduleClass.newInstance();

                muleContext.getRegistry().registerObject(moduleClass.getName(), module);
            } catch (RegistrationException e) {
                throw new InitialisationException(
                        CoreMessages.initialisationFailure(String.format("Cannot create a new instance of '%s'", moduleClass.getClass())), this);
            } catch (InstantiationException e) {
                throw new InitialisationException(
                        CoreMessages.initialisationFailure(String.format("Cannot create a new instance of '%s'", moduleClass.getClass())), this);
            } catch (IllegalAccessException e) {
                throw new InitialisationException(
                        CoreMessages.initialisationFailure(String.format("Cannot create a new instance of '%s'", moduleClass.getClass())), this);
            }
        }
    }

    public void setMuleContext(MuleContext context) {
        muleContext = context;
    }
}
