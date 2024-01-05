
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
import it.water.core.api.repository.query.QueryFilterOperation;
import it.water.core.api.repository.query.operands.FieldValueOperand;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * @Author Aristide Cittadino.
 * Abstract rule operation which maps every possible operation expression
 */
public abstract class AbstractOperation implements QueryFilterOperation, Query {
    protected Logger log = LoggerFactory.getLogger(getClass().getName());

    protected List<Query> operands;

    @Getter
    private String name;
    @Getter
    private String operator;
    private boolean needExpr;
    private int numOperands;

    protected AbstractOperation(String name, String operator, int numOperands, boolean needExpr) {
        super();
        this.name = name;
        this.operator = operator;
        this.needExpr = needExpr;
        this.operands = new ArrayList<>();
        this.numOperands = numOperands;
    }

    protected AbstractOperation() {
        this(null, null, 1, false);
    }


    /**
     * Define operation operands
     */
    @Override
    public void defineOperands(Query... operands) {
        //num Operands < 0 means unbounded
        if (this.numOperands() > 0 && operands.length > this.numOperands())
            throw new IllegalArgumentException("Too much operands for operation!");
        this.operands.clear();
        this.operands.addAll(Arrays.asList(operands));
    }

    /**
     * Boolean which says if the current operation expects a single value or an
     * expression
     */
    @Override
    public boolean needsExpr() {
        return needExpr;
    }

    @Override
    public String operator() {
        return operator;
    }

    @Override
    public int numOperands() {
        return numOperands;
    }


    @Override
    public Query and(Query rightQuery) {
        if (rightQuery == null)
            throw new IllegalArgumentException("right and operand cannot be null");
        AndOperation and = new AndOperation();
        and.defineOperands(this, rightQuery);
        return and;
    }

    @Override
    public Query or(Query rightQuery) {
        if (rightQuery == null)
            throw new IllegalArgumentException("right and operand cannot be null");
        OrOperation or = new OrOperation();
        or.defineOperands(this, rightQuery);
        return or;
    }

    @Override
    public Query not() {
        NotOperation not = new NotOperation();
        not.defineOperands(this);
        return not;
    }

    @Override
    public Query in(List<?> values) {
        In in = new In();
        //wrapping input inside an array
        Query[] inOperands = new Query[values.size() + 1];
        inOperands[0] = this;
        for (int i = 0; i < values.size(); i++) {
            int j = i + 1;
            Object value = values.get(i);
            if (!(value instanceof FieldValueOperand))
                inOperands[j] = new FieldValueOperand(value);
            else
                inOperands[j] = (Query) value;
        }
        in.defineOperands(inOperands);
        return in;
    }

    public Query getOperand(int index) {
        if (index < operands.size())
            return operands.get(index);
        return null;
    }

    public List<Query> operands(){
        return this.operands;
    }
}
