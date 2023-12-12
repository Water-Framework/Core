
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
import it.water.core.api.repository.query.operations.AbstractOperation;


/**
 * @Author Aristide Cittadino
 * Rule Node which maps parenthesis concept.
 */
public class ParenthesisNode extends AbstractOperation implements Query {
    /**
     * return the string rappresentaion of the rule
     */
    @Override
    public String getDefinition() {
        return "( " + this.operands.get(0).getDefinition() + " )";
    }
}
