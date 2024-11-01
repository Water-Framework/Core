
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

package it.water.core.api.interceptors;

import it.water.core.api.service.Service;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;


/**
 * @param <A> Annotation
 *            Used for defining annotation for pre-processing execution after a method invocation
 * @Author Aristide Cittadino
 */
public interface BeforeMethodInterceptor<A extends Annotation> extends MethodInterceptor<A> {
    /**
     * @param destination  Service which is going to be invoked
     * @param m           Method
     * @param args        Method arguments
     * @param annotation  Annotation processed on the method which maps the Interceptor definition
     * @param <S>          Service Type
     */
    <S extends Service> void interceptMethod(S destination, Method m, Object[] args, A annotation);

}
