
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

import it.water.core.api.interceptors.InterceptorExecutor;
import it.water.core.interceptors.annotations.implementation.WaterComponentsInjector;
import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * @Author Aristide Cittadino
 * This annotation tells the current the system to inject some kind of field (if found inside the component registry).
 * NOTE: WaterInject is intended to use inside framework components not outside. Spring,OSGi,Quarkus have their own injection methods.
 * Please use only if you are developing a cross-framework component and only for water components.
 */
@Target({ElementType.FIELD})
@Retention(value = RetentionPolicy.RUNTIME)
@InterceptorExecutor(interceptor = WaterComponentsInjector.class)
@IndexAnnotated
public @interface Inject {
    /**
     * means that the injection should happen only at creation time, not done dinamically
     * this happens for global components that exists for the entire application life
     *
     * @return true if the component should be injected statically
     */
    boolean injectOnceAtStartup() default false;
}
