
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

package it.water.core.api.repository.query.operands;


import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.operations.*;

/**
 * @Author Aristide Cittadino.
 * Field Rule Operand, this is a special operand which contains a field name.
 */
public class FieldNameOperand extends AbstractOperand<String> implements Query {

    /**
     * @param value
     */
    public FieldNameOperand(String value) {
        super(value);
    }

    @Override
    public String getDefinition() {
        return value;
    }

    public Query equalTo(Object value) {
        BinaryOperation op = new EqualTo();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    public Query notEqualTo(Object value) {
        BinaryOperation op = new NotEqualTo();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    public Query greaterThan(Number value) {
        BinaryOperation op = new GreaterThan();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    public Query greaterOrEqualThan(Number value) {
        BinaryOperation op = new GreaterOrEqualThan();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    public Query lowerThan(Number value) {
        BinaryOperation op = new LowerThan();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    public Query lowerOrEqualThan(Number value) {
        BinaryOperation op = new LowerOrEqualThan();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    public Query like(String value) {
        BinaryOperation op = new Like();
        op.defineOperands(this, new FieldValueOperand(value));
        return op;
    }

    @Override
    public Query and(Query rightCondition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query or(Query rightCondition) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Query not() {
        throw new UnsupportedOperationException();
    }
}
