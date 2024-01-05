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
package it.water.core.validation.javax;


import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.validation.ValidationError;
import it.water.core.validation.javax.validators.WaterJavaxValidator;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.validation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class WaterValidationTest {
    private static WaterJavaxValidator validator = new WaterJavaxValidator();

    @Test
    void testValidationSuccess() {
        TestResource testResource = new TestResource();
        testResource.setField("<html><body>Simple text</body></html>");
        testResource.setField2(2);
        Assertions.assertDoesNotThrow(() -> validator.validate(testResource));
    }

    @Test
    void testValidationFail() {
        TestResource testResource = new TestResource();
        testResource.setField("<html><body>Simple text</body><script>function(){alert('ciao')}</script></html>");
        testResource.setField2(3);
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field", "field2");
        }
        testResource.setField(null);
        testResource.setField2(null);
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field", "field2");
        }
    }

    @Test
    void tesMalitiusCodeValidations() {
        TestResource testResource = new TestResource();
        testResource.setField2(2);
        testResource.setField("' OR 1=1; --");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("'; DROP TABLE users; --");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("admin'--");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("1; DROP TABLE users;");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("1'; DROP TABLE users; --");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("') OR 1=1; --");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("') OR '1'='1'; -");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("') OR ('1'='1'; --");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("ciao");
        Assertions.assertDoesNotThrow(() -> validator.validate(testResource));


        testResource.setField("<script>alert(\"XSS\");</script>");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("a href=\"javascript:alert('XSS')\">Click me</a>");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

        testResource.setField("<img src=\"x\" onerror=\"alert('XSS')\">");
        try {
            validator.validate(testResource);
        } catch (ValidationException e) {
            checkViolations(true, e.getViolations(), "field");
        }

    }

    private void checkViolations(boolean assertViolationIsPresent, List<ValidationError> violations, String... fields) {
        //check each passed field name is inside violations
        Arrays.stream(fields).forEach(field -> {
            if (assertViolationIsPresent)
                Assertions.assertTrue(violations.stream().filter(violation -> violation.getField().contains(field)).findAny().isPresent());
            else
                Assertions.assertFalse(violations.stream().filter(violation -> violation.getField().contains(field)).findAny().isPresent());
        });

    }

}
