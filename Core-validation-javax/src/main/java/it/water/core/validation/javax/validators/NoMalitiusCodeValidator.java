
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

package it.water.core.validation.javax.validators;

import it.water.core.validation.javax.annotations.NoMalitiusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;


/**
 * @Author Aristide Cittadino.
 * Implementation of @NoMalitiusCode.
 */
public class NoMalitiusCodeValidator extends NoMalitiusCodeAbstractValidator implements ConstraintValidator<NoMalitiusCode, String> {
    private Logger log = LoggerFactory.getLogger(NoMalitiusCodeValidator.class.getName());

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return this.validate(value, () -> {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                            "{it.water.validator.nomalitiuscode.message}")
                    .addConstraintViolation();
            log.debug("@NoMalitiusCode validation failed...");
        });
    }
}
