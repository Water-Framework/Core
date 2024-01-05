
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

import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryFilterOperator;


/**
 * @Author Aristide Cittadino.
 * Not operator
 */
public class NotOperation extends UnaryOperation implements Query, QueryFilterOperator {

    public NotOperation() {
        super("Not", "NOT", 1, false);
    }

    /**
     * return the string rappresentaion of the rule
     */
    @Override
    public String getDefinition() {
        StringBuilder sb = new StringBuilder();
        sb.append("NOT (").append(this.operands.get(0).getDefinition()).append(")");
        return sb.toString();
    }
}
