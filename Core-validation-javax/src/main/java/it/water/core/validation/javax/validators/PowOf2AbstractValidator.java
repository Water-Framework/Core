
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.core.validation.javax.validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @Author Aristide Cittadino.
 * PowOf2 implementation.
 */
public abstract class PowOf2AbstractValidator {
    private Logger log = LoggerFactory.getLogger(PowOf2AbstractValidator.class.getName());

    public boolean validate(Number value, Runnable defineViolations) {
        log.debug("Validating value with @PowOf2 with value: {}", value);
        if (value == null)
            return false;
        int intValue = value.intValue();
        boolean isValid = intValue > 0 && Math.round(value.doubleValue()) == intValue && (intValue == 1 || intValue % 2 == 0);
        if (!isValid) {
            defineViolations.run();
        }
        return isValid;
    }

}
