
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
 * @Author Aristide Cittadino.
 * Abstract type for binary operations
 */
public abstract class BinaryOperation extends AbstractOperation {

    protected BinaryOperation(String name, String operator, boolean needExpr) {
        super(name, operator, 2, needExpr);
    }

    protected BinaryOperation(String name, String operator, int numOperands, boolean needExpr) {
        super(name, operator, numOperands, needExpr);
    }

    /**
     *
     */
    @Override
    public String getDefinition() {
        StringBuilder sb = new StringBuilder();
        return sb.append(operands.get(0).getDefinition())
                .append(" ")
                .append(this.operator())
                .append(" ")
                .append(operands.get(1).getDefinition())
                .toString();
    }
}
