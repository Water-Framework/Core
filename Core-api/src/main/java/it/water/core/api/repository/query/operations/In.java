
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

package it.water.core.api.repository.query.operations;

import it.water.core.api.repository.query.Query;


/**
 * @Author Aristide Cittadino
 * Like operation
 */
public class In extends BinaryValueListOperation implements Query {

    public In() {
        super("IN (in)", "IN");
    }

    @Override
    public String getDefinition() {
        StringBuilder sb = new StringBuilder();
        sb.append(operands.get(0).getDefinition())
                .append(" ")
                .append(this.operator())
                .append(" (");
        for (int i = 1; i < operands.size(); i++) {
            sb.append(operands.get(i).getDefinition());
            if (i < operands.size() - 1)
                sb.append(",");
            else
                sb.append(")");
        }
        return sb.toString();
    }

}
