
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

package it.water.core.validation.validators;

import it.water.core.validation.annotations.PowOf2;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author Aristide Cittadino.
 * PowOf2 implementation.
 */
public class PowOf2Validator extends PowOf2AbstractValidator implements ConstraintValidator<PowOf2, Number> {
    private Logger log = LoggerFactory.getLogger(PowOf2Validator.class.getName());

    @Override
    public boolean isValid(Number value, ConstraintValidatorContext context) {
        return this.validate(value, () -> {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("{it.water.validator.powOf2.message}")
                    .addConstraintViolation();
            log.debug("@PowOf2 validation failed: {}", value);
        });
    }

}
