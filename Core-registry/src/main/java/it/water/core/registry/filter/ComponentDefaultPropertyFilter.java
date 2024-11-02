
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
import it.water.core.api.registry.filter.ComponentPropertyFilter;
import it.water.core.api.registry.filter.FilterImplementation;
import lombok.Getter;
import lombok.Setter;

import java.util.Properties;

/**
 * @Author Aristide Cittadino
 * OSGi Filter Property filter
 */
public class ComponentDefaultPropertyFilter extends AbstractBinaryComponentFilter implements ComponentPropertyFilter {

    @Getter
    @Setter
    private String name;
    @Getter
    @Setter
    private String value;

    public ComponentDefaultPropertyFilter(String name, String value, FilterImplementation filterImplementation) {
        super(null,null,filterImplementation);
        this.name = name;
        this.value = value;
    }

    @Override
    public boolean matches(Properties props) {
        boolean matches = props.containsKey(name) && props.getProperty(name).equals(value);
        return (this.not) ? !matches : matches;
    }

}
