
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

import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.api.registry.filter.ComponentFilterAndCondition;
import it.water.core.api.registry.filter.FilterImplementation;
import lombok.Getter;

import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Filter And Condition
 */
public class ComponentDefaultFilterAndCondition extends AbstractBinaryComponentFilter implements ComponentFilterAndCondition {

    public ComponentDefaultFilterAndCondition(ComponentFilter first, ComponentFilter second, FilterImplementation implementation) {
        super(first, second, implementation);
    }

    @Override
    public boolean matches(Properties props) {
        boolean matches = first.matches(props) && second.matches(props);
        return (this.not) ? !matches : matches;
    }

}
