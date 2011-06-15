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

package org.mule.devkit.apt.validation;

import org.mule.devkit.annotations.Configurable;
import org.mule.devkit.validation.TypeValidator;
import org.mule.devkit.validation.ValidationException;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
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