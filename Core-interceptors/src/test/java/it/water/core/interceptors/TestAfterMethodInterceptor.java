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
package it.water.core.interceptors;

import it.water.core.api.interceptors.AfterMethodInterceptor;
import it.water.core.api.service.Service;
import it.water.core.interceptors.annotations.FrameworkComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

/**
 * @Author Aristide Cittadino
 * This class is used to register a before method interceptor via component registration.
 */
@FrameworkComponent(services = AfterMethodInterceptor.class)
public class TestAfterMethodInterceptor implements AfterMethodInterceptor<TestMethodAnnotation> {
    private static Logger log = LoggerFactory.getLogger(TestAfterMethodInterceptor.class);

    @Override
    public <S extends Service> void interceptMethod(S destination, Method m, Object[] args, Object returnResult, TestMethodAnnotation annotation) {
        log.info("#### METHOD INTERCEPTED! ####");
    }
}
