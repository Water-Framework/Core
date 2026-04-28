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

package it.water.core.service.integration.discovery;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryMetadataProvider;
import it.water.core.api.service.integration.discovery.RestApiServiceRegistrationLifecycleManager;
import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.core.api.service.rest.FrameworkRestController;
import it.water.core.interceptors.annotations.FrameworkComponent;
import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Registers REST business APIs from {@link FrameworkRestApi} metadata instead of runtime descriptors.
 */
@FrameworkComponent(services = RestApiServiceRegistrationLifecycleManager.class)
public class RestApiServiceRegistrationLifecycleManagerImpl implements RestApiServiceRegistrationLifecycleManager {
    private static final Logger log = LoggerFactory.getLogger(RestApiServiceRegistrationLifecycleManagerImpl.class);
    private static final String PATH_ANNOTATION_CLASS_NAME = "javax.ws.rs.Path";
    private static final String REST_CONTROLLER_ANNOTATION_CLASS_NAME =
            "it.water.core.api.service.rest.FrameworkRestController";

    private final Map<String, RestApiServiceRegistrationLifecycle> activeRegistrations = new LinkedHashMap<>();

    @Override
    public synchronized void activateRestApiRegistrations(ComponentRegistry componentRegistry, ClassLoader classLoader) {
        if (componentRegistry == null || classLoader == null) {
            return;
        }

        ApplicationProperties applicationProperties = findComponentQuietly(componentRegistry, ApplicationProperties.class);
        if (applicationProperties == null) {
            log.warn("ApplicationProperties not available, skipping REST API service registration lifecycle");
            return;
        }
        String discoveryUrl = applicationProperties.getPropertyOrDefault(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, "");
        if (discoveryUrl.isBlank()) {
            log.debug("REST API service registration disabled: {} not configured",
                    ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL);
            return;
        }

        Map<Class<?>, Class<?>> restApisToRegister = findRestApisToRegister(classLoader);
        restApisToRegister.forEach((restApiClass, controllerClass) -> {
            String root = resolvePath(restApiClass);
            if (!isBusinessRoot(root)) {
                return;
            }
            String serviceName = resolveServiceName(componentRegistry, restApiClass, controllerClass);
            if (serviceName.isBlank()) {
                return;
            }
            String key = serviceName + ":" + root;
            if (activeRegistrations.containsKey(key)) {
                return;
            }

            log.info("Activating REST API service registration for '{}' at root '{}'", serviceName, root);
            RestApiServiceRegistrationLifecycle registration = new RestApiServiceRegistrationLifecycle();
            registration.activate(componentRegistry,
                    new RestApiServiceRegistrationOptions(serviceName, root, applicationProperties));
            activeRegistrations.put(key, registration);
        });
    }

    @OnDeactivate
    public synchronized void deactivate() {
        activeRegistrations.values().forEach(RestApiServiceRegistrationLifecycle::deactivate);
        activeRegistrations.clear();
    }

    String deriveServiceName(Class<?> restApiClass) {
        if (restApiClass == null) {
            return "";
        }
        return ServiceDiscoveryMetadataProvider.deriveServiceName(restApiClass.getSimpleName());
    }

    boolean isBusinessRoot(String root) {
        if (root == null || root.isBlank()) {
            return false;
        }
        String normalizedRoot = DiscoveryAddressUtils.normalizeRoot(root);
        return !"/".equals(normalizedRoot)
                && !normalizedRoot.startsWith("/api")
                && !normalizedRoot.startsWith("/internal")
                && !normalizedRoot.startsWith("/proxy")
                && !normalizedRoot.startsWith("/status");
    }

    private String resolvePath(Class<?> restApiClass) {
        if (restApiClass == null) {
            return "";
        }
        for (Annotation annotation : restApiClass.getAnnotations()) {
            if (PATH_ANNOTATION_CLASS_NAME.equals(annotation.annotationType().getName())) {
                try {
                    Method valueMethod = annotation.annotationType().getMethod("value");
                    Object value = valueMethod.invoke(annotation);
                    return value != null ? value.toString() : "";
                } catch (Exception e) {
                    log.warn("Unable to read @Path value from {}: {}", restApiClass.getName(), e.getMessage());
                    return "";
                }
            }
        }
        return "";
    }

    private Map<Class<?>, Class<?>> findRestApisToRegister(ClassLoader classLoader) {
        Map<Class<?>, Class<?>> restApisToRegister = new LinkedHashMap<>();
        findAnnotatedClasses(FrameworkRestController.class, classLoader).forEach(controllerClass -> {
            Class<?> restApiClass = resolveReferredRestApi(controllerClass);
            if (restApiClass != null) {
                restApisToRegister.put(restApiClass, controllerClass);
            }
        });
        // Keep direct RestApi scanning as a fallback for runtimes/tests where the API interface is indexed
        // but the controller lives in a different classloader.
        findAnnotatedClasses(FrameworkRestApi.class, classLoader).forEach(restApiClass ->
                restApisToRegister.putIfAbsent(restApiClass, null));
        log.debug("REST API service registration scan found {} candidate(s)", restApisToRegister.size());
        return restApisToRegister;
    }

    private Set<Class<?>> findAnnotatedClasses(Class<? extends Annotation> annotation, ClassLoader classLoader) {
        Set<Class<?>> annotatedClasses = new LinkedHashSet<>();
        ClassIndex.getAnnotated(annotation, classLoader).forEach(annotatedClasses::add);
        return annotatedClasses;
    }

    private Class<?> resolveReferredRestApi(Class<?> controllerClass) {
        if (controllerClass == null) {
            return null;
        }
        for (Annotation annotation : controllerClass.getAnnotations()) {
            if (REST_CONTROLLER_ANNOTATION_CLASS_NAME.equals(annotation.annotationType().getName())) {
                try {
                    Method referredRestApiMethod = annotation.annotationType().getMethod("referredRestApi");
                    Object referredRestApi = referredRestApiMethod.invoke(annotation);
                    return referredRestApi instanceof Class<?> ? (Class<?>) referredRestApi : null;
                } catch (Exception e) {
                    log.warn("Unable to read @FrameworkRestController referredRestApi from {}: {}",
                            controllerClass.getName(), e.getMessage());
                    return null;
                }
            }
        }
        log.debug("Skipping {} because @FrameworkRestController is not available at runtime",
                controllerClass.getName());
        return null;
    }

    private String resolveServiceName(ComponentRegistry componentRegistry, Class<?> restApiClass, Class<?> controllerClass) {
        String providedServiceName = resolveServiceNameFromController(componentRegistry, controllerClass);
        if (!providedServiceName.isBlank()) {
            return providedServiceName;
        }
        return deriveServiceName(restApiClass);
    }

    private String resolveServiceNameFromController(ComponentRegistry componentRegistry, Class<?> controllerClass) {
        if (componentRegistry == null || controllerClass == null) {
            return "";
        }
        for (Field field : controllerClass.getDeclaredFields()) {
            if (!isInjectField(field) || ComponentRegistry.class.isAssignableFrom(field.getType())) {
                continue;
            }
            Object component = findComponentQuietly(componentRegistry, field.getType());
            if (component == null) {
                continue;
            }
            if (component instanceof ServiceDiscoveryMetadataProvider) {
                String serviceName = ((ServiceDiscoveryMetadataProvider) component).getServiceName();
                if (serviceName != null && !serviceName.isBlank()) {
                    return serviceName;
                }
            }
            String serviceNameFromInjectedApi = ServiceDiscoveryMetadataProvider.deriveServiceName(field.getType().getSimpleName());
            if (!serviceNameFromInjectedApi.isBlank()) {
                return serviceNameFromInjectedApi;
            }
        }
        return "";
    }

    private boolean isInjectField(Field field) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            if ("it.water.core.interceptors.annotations.Inject".equals(annotation.annotationType().getName())) {
                return true;
            }
        }
        return false;
    }

    private <T> T findComponentQuietly(ComponentRegistry componentRegistry, Class<T> componentClass) {
        try {
            return componentRegistry.findComponent(componentClass, null);
        } catch (Exception e) {
            log.debug("Component {} not available: {}", componentClass.getName(), e.getMessage());
            return null;
        }
    }
}
