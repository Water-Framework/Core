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
package it.water.core.testing.utils.bundle;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.bundle.Runtime;
import it.water.core.api.model.User;
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.service.rest.FrameworkRestController;
import it.water.core.bundle.RuntimeInitializer;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.core.testing.utils.filter.TestComponentFilterBuilder;
import it.water.core.testing.utils.registry.TestComponentRegistration;
import it.water.core.testing.utils.registry.TestComponentRegistry;
import it.water.core.testing.utils.security.TestSecurityContext;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class TestRuntimeInitializer extends RuntimeInitializer<Object, TestComponentRegistration<Object>> {
    private ComponentRegistry componentRegistry;
    private ApplicationProperties waterApplicationProperties;
    private static TestRuntimeInitializer instance;

    private TestRuntimeInitializer() {
    }

    public static TestRuntimeInitializer getInstance() {
        if (instance == null) {
            instance = new TestRuntimeInitializer();
        }
        return instance;
    }

    @Override
    public ComponentRegistry getComponentRegistry() {
        if (componentRegistry == null) componentRegistry = new TestComponentRegistry();
        return componentRegistry;
    }

    public void start() {
        this.setupApplicationProperties();
        //registering test component filter builder
        ComponentConfiguration config = ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(1).build();
        this.getComponentRegistry().registerComponent(ComponentFilterBuilder.class, new TestComponentFilterBuilder(), config);
        this.initializeFrameworkComponents();
        this.initializeResourcePermissionsAndActions();
        this.initializeRestApis();
    }

    private void setupApplicationProperties() {
        this.waterApplicationProperties = new TestApplicationProperties();
        ComponentConfiguration configuration = ComponentConfigurationFactory.createNewComponentPropertyFactory().build();
        waterApplicationProperties.setup();
        getComponentRegistry().registerComponent(ApplicationProperties.class, waterApplicationProperties, configuration);
    }

    public TestApplicationProperties getApplicationProperties() {
        return (TestApplicationProperties) waterApplicationProperties;
    }

    public void impersonate(User u) {
        Runtime runtime = this.getComponentRegistry().findComponent(Runtime.class, null);
        runtime.fillSecurityContext(TestSecurityContext.createContext(u.getId(), u.getUsername(), u.isAdmin()));
    }

    public PermissionManager getPermissionManager() {
        return this.getComponentRegistry().findComponent(PermissionManager.class, null);
    }

    public boolean hasRestApi() {
        return getAnnotatedClasses(FrameworkRestController.class).iterator().hasNext();
    }

    /**
     * In test projects it is ok to scan all packages since the amount of classes to scan for each module
     * should be small. For runtime it's better to use libraries which index annotated classes such as
     * ClassIndex used by default.
     *
     * @param annotationClass Annotation that has to be searched
     * @return
     */
    @Override
    protected Iterable<Class<?>> getAnnotatedClasses(Class annotationClass) {
        ConfigurationBuilder builder = new ConfigurationBuilder();
        builder.setUrls(ClasspathHelper.forJavaClassPath());
        builder.setExpandSuperTypes(true);
        builder.setParallel(true);
        Reflections reflections = new Reflections(builder);
        return reflections.getTypesAnnotatedWith(annotationClass);
    }


}
