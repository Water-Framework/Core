
/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package it.water.core.validation.annotations;

import it.water.core.model.validation.ValidationMessages;
import it.water.core.validation.validators.PowOf2Validator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @Author Aristide Cittadino.
 * Simple validator which verifies that inserted value is a pow of 2.
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PowOf2Validator.class)
public @interface PowOf2 {
    String message() default ValidationMessages.POW_OF_2_VALIDATION;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
