
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

package it.water.core.api.registry.filter;

public interface FilterImplementation {
    default String transform(ComponentFilter filter) {
        if (filter instanceof ComponentFilterAndCondition)
            return transform((ComponentFilterAndCondition) filter);
        else if (filter instanceof ComponentFilterOrCondition)
            return transform((ComponentFilterOrCondition) filter);
        else if (filter instanceof ComponentPropertyFilter)
            return transform((ComponentPropertyFilter) filter);
        else
            throw new IllegalArgumentException("Invalid filter");
    }

    String transform(ComponentFilterAndCondition andCondition);

    String transform(ComponentFilterOrCondition orCondition);

    String transform(ComponentPropertyFilter propertyFilter);
}
