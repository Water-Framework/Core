
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

import lombok.Getter;

import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Filter OR Condition
 */
public class ComponentDefaultFilterOrCondition extends AbstractComponentFilter implements ComponentFilterOrCondition {
    @Getter
    private ComponentFilter first;
    @Getter
    private ComponentFilter second;
    @Getter
    private boolean not;

    public ComponentDefaultFilterOrCondition(ComponentFilter first, ComponentFilter second, FilterImplementation filterImplementation) {
        super(filterImplementation);
        this.first = first;
        this.second = second;
    }

    @Override
    public ComponentFilter and(ComponentFilter filter) {
        return new ComponentDefaultFilterOrCondition(this, filter, getFilterImplementation());
    }

    @Override
    public ComponentFilter and(String propertyName, String propertyValue) {
        return new ComponentDefaultFilterAndCondition(this, new ComponentDefaultPropertyFilter(propertyName, propertyValue, getFilterImplementation()), getFilterImplementation());
    }

    @Override
    public ComponentFilter or(ComponentFilter filter) {
        return new ComponentDefaultFilterOrCondition(this, filter, getFilterImplementation());
    }

    @Override
    public ComponentFilter or(String propertyName, String propertyValue) {
        return new ComponentDefaultFilterOrCondition(this, new ComponentDefaultPropertyFilter(propertyName, propertyValue, getFilterImplementation()), getFilterImplementation());
    }

    @Override
    public ComponentFilter not() {
        not = true;
        return this;
    }

    @Override
    public boolean matches(Properties props) {
        boolean matches = first.matches(props) || second.matches(props);
        return (this.not) ? !matches : matches;
    }

}
