
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

package it.water.core.model;

import it.water.core.api.model.ErrorMessage;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * @Author Aristide Cittadino.
 * Model class for BaseError.
 * It is used to map, all error messages produced by exceptions during interaction with the  platform.
 */
public class BaseError {
    protected static Logger log = LoggerFactory.getLogger(BaseError.class.getName());

    /**
     * int status code of error
     */
    @Getter
    @Setter
    private int statusCode;
    /**
     * String type of error
     */
    @Getter
    @Setter
    private String type;
    /**
     * List of error messages
     */
    @Getter
    @Setter
    private List<ErrorMessage> errorMessages;


    /**
     * Empty constructor for BaseError
     */
    public BaseError() {
        this.errorMessages = new ArrayList<>();
    }

    /**
     * Constructor with parameters for BaseError
     *
     * @param statusCode    parameter that indicates status code of error
     * @param type          parameter that indicates type of error
     * @param errorMessages parameter that indicates list of error messages
     */
    public BaseError(int statusCode, String type, List<ErrorMessage> errorMessages) {
        this.statusCode = statusCode;
        this.type = type;
        this.errorMessages = errorMessages;
    }

    /**
     * Constructor with parameters for BaseError
     *
     * @param statusCode parameter that indicates status code of error
     * @param type       parameter that indicates type of error
     */
    public BaseError(int statusCode, String type) {
        this.statusCode = statusCode;
        this.type = type;
        this.errorMessages = new ArrayList<>();
    }

    /**
     * Return errors produced during interaction with  platform
     *
     * @param t          parameter that indicates all exceptions in
     *                   platform
     * @param messages   parameter that indicates list of error messages
     * @param statusCode parameter that indicates status code of error
     * @return errors and exceptions produced during interaction with
     * platform
     */
    public static BaseError generateError(Throwable t, List<ErrorMessage> messages, int statusCode) {
        log.debug("Invoking generateError with throwable : {} {} {}", t, messages, statusCode);
        BaseError errorResponse = new BaseError();
        errorResponse.setType(t.getClass().getName());
        if (messages != null) {
            errorResponse.setErrorMessages(messages);
        }
        if (statusCode > 0) {
            errorResponse.setStatusCode(statusCode);
        }
        return errorResponse;
    }

    /**
     * Return errors produced during interaction with  platform
     *
     * @param t          parameter that indicates all exceptions in Water
     *                   platform
     * @param statusCode parameter that indicates status code of error
     * @return errors and exceptions produced during interaction with Water
     * platform
     */
    public static BaseError generateError(Throwable t, int statusCode) {
        log.debug("Invoking generateError with throwable : {}, {}", t, statusCode);
        return generateError(t, Collections.emptyList(), statusCode);
    }
}
