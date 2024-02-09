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
import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import org.junit.jupiter.api.Assertions;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class FakeInterceptor extends WaterAbstractInterceptor implements Service, BeforeMethodInterceptor, AfterMethodInterceptor {

    private ComponentRegistry registry;

    public FakeInterceptor(ComponentRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected ComponentRegistry getComponentsRegistry() {
        return registry;
    }

    @Override
    public Service getService() {
        return super.getService();
    }

    @Override
    public void setService(Service service) {
        super.setService(service);
    }

    @Override
    public void interceptMethod(Service destination, Method m, Object[] args, Object returnResult, Annotation annotation) {
        try {
            executeInterceptorAfterMethod(destination, m, args, returnResult);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    @Override
    public void interceptMethod(Service destination, Method m, Object[] args, Annotation annotation) {
        try {
            executeInterceptorBeforeMethod(destination, m, args);
        } catch (Exception e) {
            e.printStackTrace();
            Assertions.assertTrue(false);
        }
    }

    @Override
    public Class getAnnotation() {
        return null;
    }
}
