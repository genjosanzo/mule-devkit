package org.mule.devkit.apt.validation;

import org.mule.devkit.annotations.Configurable;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class ModuleTypeValidator implements TypeValidator {

    public void validate(TypeElement type) throws ValidationException {
        // must not be an interface
        if (type.getKind() == ElementKind.INTERFACE) {
            throw new ValidationException(type, "@Module cannot be applied to an interface");
        }

        // must not have type parameters
        if (!type.getTypeParameters().isEmpty()) {
            throw new ValidationException(type, "@Module type cannot have type parameters");
        }

        // must be public
        if (!type.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ValidationException(type, "@Module must be public");
        }

        //
        List<VariableElement> variables = ElementFilter.fieldsIn(type.getEnclosedElements());
        for( VariableElement variable : variables )
        {
            Configurable configurable = variable.getAnnotation(Configurable.class);
            if( configurable == null )
                continue;

            if( variable.getModifiers().contains(Modifier.FINAL) )
            {
                throw new ValidationException(variable, "@Configurable cannot be applied to field with final modifier");
            }

            if( variable.getModifiers().contains(Modifier.STATIC) )
            {
                throw new ValidationException(variable, "@Configurable cannot be applied to field with static modifier");
            }

            if( variable.asType().getKind().isPrimitive() && configurable.optional() && configurable.defaultValue().length() == 0 )
            {
                throw new ValidationException(variable, "Optional configurable fields can only be applied to non-primitive types without a default value");
            }
        }
    }
}
