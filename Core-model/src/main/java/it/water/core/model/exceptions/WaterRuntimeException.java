
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

package it.water.core.model.exceptions;

import lombok.NoArgsConstructor;

/**
 * @Author Aristide Cittadino.
 * Model class for RuntimeException. It is
 * used to map all error messages produced by runtime exceptions.
 */
@NoArgsConstructor
public class WaterRuntimeException extends java.lang.RuntimeException {

    /**
     * A unique serial version identifier
     */
    private static final long serialVersionUID = 1L;


    /**
     * Constructor for RuntimeException with the specified detail message,
     * cause, suppression enabled or disabled, and writable stack trace enabled or
     * disabled.
     *
     * @param message            parameter that indicates the detail message
     * @param cause              parameter that indicates the cause of runtime
     *                           exception
     * @param enableSuppression  parameter that determines if the suppression is
     *                           enable or not
     * @param writableStackTrace parameter that determines if the stack trace should
     *                           be writable
     */
    public WaterRuntimeException(String message, Throwable cause, boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * Constructor for RuntimeException with the specified detail message,
     * cause.
     *
     * @param message parameter that indicates the detail message
     * @param cause   parameter that indicates the cause of runtime exception
     */
    public WaterRuntimeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor for RuntimeException with the specified detail message.
     *
     * @param message parameter that indicates the detail message
     */
    public WaterRuntimeException(String message) {
        super(message);
    }

    /**
     * Constructor for RuntimeException with the specified cause of runtime
     * exception.
     *
     * @param cause parameter that indicates the cause of runtime exception
     */
    public WaterRuntimeException(Throwable cause) {
        super(cause);
    }

}
