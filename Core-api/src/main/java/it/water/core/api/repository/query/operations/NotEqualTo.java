
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

package it.water.core.api.repository.query.operations;

/**
 * @Author Aristide Cittadino
 * Not equal to operation
 */
public class NotEqualTo extends BinaryValueOperation {

    public NotEqualTo() {
        super("NotEqualTo (!=)", "<>");
    }

    /**
     * Returns the operation name
     */
    @Override
    public String getName() {
        return "NotEqualTo (!=)";
    }

    /**
     * returns the token string which identifies the operation
     */
    @Override
    public String operator() {
        return "<>";
    }

}
