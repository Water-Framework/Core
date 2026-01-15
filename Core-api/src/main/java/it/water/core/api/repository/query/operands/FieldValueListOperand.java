
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

package it.water.core.api.repository.query.operands;

import java.util.List;

/**
 * @Author Aristide Cittadino.
 * Field Value operand which contains a specific value for a field.
 */
public class FieldValueListOperand extends AbstractOperand<List<Object>> {

    public FieldValueListOperand(List<Object> value) {
        super(value);
    }

    public void addValue(Object valueElement){
        this.value.add(valueElement);
    }

    @Override
    public String getDefinition() {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < this.value.size(); i++){
            sb.append(this.value.get(i).toString());
            if(i < this.value.size()-1)
                sb.append(",");
        }
        return sb.toString();
    }
}
