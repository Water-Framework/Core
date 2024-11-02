
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
import it.water.core.api.registry.filter.ComponentFilterOrCondition;
import it.water.core.api.registry.filter.FilterImplementation;

import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Filter OR Condition
 */
public class ComponentDefaultFilterOrCondition extends AbstractBinaryComponentFilter implements ComponentFilterOrCondition {

    public ComponentDefaultFilterOrCondition(ComponentFilter first, ComponentFilter second, FilterImplementation implementation) {
        super(first, second, implementation);
    }

    @Override
    public boolean matches(Properties props) {
        boolean matches = first.matches(props) || second.matches(props);
        return (this.not) ? !matches : matches;
    }

}
