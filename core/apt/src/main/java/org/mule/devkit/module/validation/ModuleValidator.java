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

package org.mule.devkit.module.validation;

import org.mule.api.annotations.Configurable;
import org.mule.api.annotations.Module;
import org.mule.api.annotations.param.Default;
import org.mule.api.annotations.param.Optional;
import org.mule.devkit.validation.ValidationException;
import org.mule.devkit.validation.Validator;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import java.util.List;

public class ModuleValidator implements Validator {

    public void validate(Element element) throws ValidationException {
        // if it not annotated just skip
        if( element.getAnnotation(Module.class) == null )
            return;

        // must not be an interface
        if (element.getKind() == ElementKind.INTERFACE) {
            throw new ValidationException(element, "@Module cannot be applied to an interface");
        }

        // must not have type parameters
        if (!((TypeElement)element).getTypeParameters().isEmpty()) {
            throw new ValidationException(element, "@Module type cannot have type parameters");
        }

        // must be public
        if (!element.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ValidationException(element, "@Module must be public");
        }

        //
        List<VariableElement> variables = ElementFilter.fieldsIn(element.getEnclosedElements());
        for( VariableElement variable : variables )
        {
            Configurable configurable = variable.getAnnotation(Configurable.class);
            if( configurable == null )
                continue;
            Optional optional = variable.getAnnotation(Optional.class);
            Default def = variable.getAnnotation(Default.class);

            if( variable.getModifiers().contains(Modifier.FINAL) )
            {
                throw new ValidationException(variable, "@Configurable cannot be applied to field with final modifier");
            }

            if( variable.getModifiers().contains(Modifier.STATIC) )
            {
                throw new ValidationException(variable, "@Configurable cannot be applied to field with static modifier");
            }

            if( variable.asType().getKind().isPrimitive() && optional != null && def.value().length() == 0 )
            {
                throw new ValidationException(variable, "Optional configurable fields can only be applied to non-primitive types without a default value");
            }
        }

        // verify that every @Processor is public and non-static and non-generic

        // verify that every @Source is public and non-static and non-generic
        // verify that every @Source receives a SourceCallback

        // verify that every @Transformer is public and non-static and non-generic
        // verify that every @Transformer signature is Object x(Object);

        // verify that every @Filter is public and non-static and non-generic
    }
}
