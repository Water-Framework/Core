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
package it.water.core.bundle;

import it.water.core.api.bundle.Runtime;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.rest.FrameworkRestController;
import it.water.core.interceptors.annotations.FrameworkComponent;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

public class BundleTestInitializer extends RuntimeInitializer<Object, Object> {

    private ComponentRegistry registry;
    private Runtime runtime;

    public BundleTestInitializer(ComponentRegistry registry, Runtime runtime) {
        this.registry = registry;
        this.runtime = runtime;
    }

    /**
     * Simulates component registration through package scan
     *
     * @param annotation Annotation that has to be searched
     * @return
     */
    @Override
    protected Iterable<Class<?>> getAnnotatedClasses(Class<? extends Annotation> annotation) {
        List<Class<?>> classes = new ArrayList<>();
        super.getAnnotatedClasses(annotation).forEach(it -> classes.add(it));
        if (annotation.getName().equals(FrameworkComponent.class.getName())) {
            classes.add(FakeFrameworkComponent.class);
            classes.add(TestComponent.class);
        } else if (annotation.getName().equals(FrameworkRestController.class.getName())) {
            classes.add(TestRestController.class);
        }
        return classes;
    }

    @Override
    protected ComponentRegistry getComponentRegistry() {
        return registry;
    }

    public void start() {
        this.initializeFrameworkComponents();
        this.initializeRestApis();
    }
}
