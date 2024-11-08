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

package it.water.core.registry.filter;

import it.water.core.api.registry.filter.ComponentFilterAndCondition;
import it.water.core.api.registry.filter.ComponentFilterOrCondition;
import it.water.core.api.registry.filter.ComponentPropertyFilter;
import it.water.core.api.registry.filter.FilterImplementation;

public abstract class ComponentAbstractFilter implements FilterImplementation {

    @Override
    public String transform(ComponentFilterAndCondition andCondition) {
        String filterCondition = "(&" + andCondition.getFirst().getFilter() + andCondition.getSecond().getFilter() + ")";
        if (!andCondition.isNot())
            return filterCondition;
        else
            return "(!" + filterCondition + ")";
    }

    @Override
    public String transform(ComponentFilterOrCondition orCondition) {
        String filterCondition = "(|" + orCondition.getFirst().getFilter() + orCondition.getSecond().getFilter() + ")";
        if (!orCondition.isNot())
            return filterCondition;
        else
            return "(!" + filterCondition + ")";
    }

    @Override
    public String transform(ComponentPropertyFilter propertyFilter) {
        String filterCondition = "(" + propertyFilter.getName() + "=" + propertyFilter.getValue() + ")";
        if (!propertyFilter.isNot()) return filterCondition;
        else return "(!" + filterCondition + ")";
    }
}
