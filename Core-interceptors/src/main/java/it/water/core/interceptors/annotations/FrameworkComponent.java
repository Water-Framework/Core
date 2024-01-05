
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

package it.water.core.interceptors.annotations;

import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Author Aristide Cittadino
 * This annotation tells the current runtime which classes should be instantiated as components.
 * In this way we abstract the concept of component from the implementation such as osgi, spring. quarkus.
 */
@Target({ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@IndexAnnotated
public @interface FrameworkComponent {
    /**
     * Component registration properties
     */
    String[] properties() default {};

    /**
     * Interfaces exposed by the component
     */
    Class<?>[] services() default {};

    /**
     * Priority is used to allows user customize all components inside the framework
     * by declaring object with the same interfaces giving higher priority.
     *
     * @return
     */
    int priority() default 1;
}
