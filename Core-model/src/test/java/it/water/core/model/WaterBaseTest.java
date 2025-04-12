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
package it.water.core.model;

import it.water.core.api.entity.owned.OwnedResource;
import it.water.core.api.model.ErrorMessage;
import it.water.core.model.exceptions.ValidationException;
import it.water.core.model.exceptions.WaterException;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.model.validation.ValidationError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

class WaterBaseTest {

    @Test
    void testRuntimeException() {
        WaterException cause = new WaterException();
        WaterRuntimeException ex = new WaterRuntimeException();
        ex = new WaterRuntimeException("Error message");
        Assertions.assertNotNull(ex.getMessage());
        ex = new WaterRuntimeException("Error message", cause);
        Assertions.assertNotNull(ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
        ex = new WaterRuntimeException(cause);
        Assertions.assertNotNull(ex.getCause());
    }

    @Test
    void testAbstractResource() {
        TestResource resource = new TestResource("field1");
        Assertions.assertNotNull(resource.getField1());
        Assertions.assertEquals(resource.getClass().getName(), resource.getResourceName());
    }

    @Test
    void testOwnedResource() {
        Assertions.assertNotNull(OwnedResource.getOwnerUserIdFieldName());
    }

    @Test
    void testBaseError() {
        BaseError exError = new BaseError();
        BasicErrorMessage message = new BasicErrorMessage("error message");
        List<ErrorMessage> errorMessages = new ArrayList<>();
        errorMessages.add(message);
        Assertions.assertEquals("error message", message.getMessage());
        exError.setType("type");
        exError.setStatusCode(404);
        exError.setErrorMessages(errorMessages);
        Assertions.assertEquals("type", exError.getType());
        Assertions.assertEquals(404, exError.getStatusCode());
        Assertions.assertEquals(1, exError.getErrorMessages().size());

    }

    @Test
    void testBaseErrorConstructorWithStatusCodeAndType() {
        BasicErrorMessage message = new BasicErrorMessage("error message");
        List<ErrorMessage> errorMessages = new ArrayList<>();
        errorMessages.add(message);
        BaseError exError = new BaseError(404, "type");
        Assertions.assertEquals("error message", message.getMessage());
        exError.setErrorMessages(errorMessages);
        Assertions.assertEquals("type", exError.getType());
        Assertions.assertEquals(404, exError.getStatusCode());
        Assertions.assertEquals(1, exError.getErrorMessages().size());

    }

    @Test
    void testBaseErrorFullConstructor() {
        BasicErrorMessage message = new BasicErrorMessage("error message");
        List<ErrorMessage> errorMessages = new ArrayList<>();
        errorMessages.add(message);
        BaseError exError = new BaseError(404, "type", errorMessages);
        Assertions.assertEquals("error message", message.getMessage());
        Assertions.assertEquals("type", exError.getType());
        Assertions.assertEquals(404, exError.getStatusCode());
        Assertions.assertEquals(1, exError.getErrorMessages().size());
    }

    @Test
    void testGenerateError() {
        BasicErrorMessage message = new BasicErrorMessage("error message");
        List<ErrorMessage> errorMessages = new ArrayList<>();
        errorMessages.add(message);
        BaseError error = BaseError.generateError(new Exception(), errorMessages, 404);
        Assertions.assertEquals(404, error.getStatusCode());
        Assertions.assertEquals(1, error.getErrorMessages().size());
        error = BaseError.generateError(new Exception(), 404);
        Assertions.assertEquals(404, error.getStatusCode());
    }

    @Test
    void testValidationException(){
        List<ValidationError> errors = new ArrayList<>();
        errors.add(new ValidationError("validation error","field","value"));
        ValidationException validationException  = new ValidationException(errors);
        Assertions.assertNotNull(validationException.getMessage());
    }
}
