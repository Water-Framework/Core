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
import it.water.core.api.permission.PermissionManager;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.bundle.RuntimeInitializer;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.core.testing.utils.filter.TestComponentFilterBuilder;
import it.water.core.testing.utils.registry.TestComponentRegistration;
import it.water.core.testing.utils.registry.TestComponentRegistry;
import it.water.core.testing.utils.runtime.TestRuntime;
import it.water.core.testing.utils.security.FakePermissionManager;
import it.water.core.testing.utils.security.TestSecurityContext;
import org.reflections.Reflections;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

public class TestInitializer extends RuntimeInitializer<Object, TestComponentRegistration<Object>> {
    private ComponentRegistry componentRegistry;
    private Runtime testRuntime;
    private PermissionManager defaultPermissionManager;
    private ApplicationProperties waterApplicationProperties;

    @Override
    public ComponentRegistry getComponentRegistry() {
        if (componentRegistry == null) componentRegistry = new TestComponentRegistry();
        return componentRegistry;
    }

    public TestInitializer withPermissionManager(PermissionManager permissionManager) {
        this.defaultPermissionManager = permissionManager;
        return this;
    }

    public TestInitializer withFakePermissionManager(boolean alwaysPass) {
        this.withPermissionManager(new FakePermissionManager(alwaysPass));
        return this;
    }

    public TestInitializer withFakePermissionManager() {
        this.withPermissionManager(new FakePermissionManager(false));
        return this;
    }

    public void start() {
        this.setupApplicationProperties();
        if (this.defaultPermissionManager != null) {
            ComponentConfiguration config = ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(1).withProp("it.water.core.security.permission.implementation", "default").build();
            this.getComponentRegistry().registerComponent(PermissionManager.class, defaultPermissionManager, config);
        }
        //registering test component filter builder
        ComponentConfiguration config = ComponentConfigurationFactory.createNewComponentPropertyFactory().withPriority(1).build();
        this.getComponentRegistry().registerComponent(ComponentFilterBuilder.class, new TestComponentFilterBuilder(), config);
        this.initializeFrameworkComponents();
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

    @Override
    public Runtime getRuntime() {
        if (testRuntime == null) testRuntime = new TestRuntime(this.waterApplicationProperties);
        return testRuntime;
    }

    public void impersonate(String username, boolean admin, long id) {
        ((TestRuntime) this.getRuntime()).switchSecurityContext(TestSecurityContext.createContext(id, username, admin, componentRegistry.findComponent(PermissionManager.class, null)));
    }

    public PermissionManager getPermissionManager() {
        return this.getComponentRegistry().findComponent(PermissionManager.class, null);
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
