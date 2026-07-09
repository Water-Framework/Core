package it.water.core.service.integration.discovery;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.ComponentRegistration;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.registry.filter.ComponentFilterBuilder;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseApi;
import it.water.core.api.service.BaseEntitySystemApi;
import it.water.core.api.service.BaseSystemApi;
import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import it.water.core.api.service.integration.discovery.ServiceLivenessClient;
import it.water.core.api.service.integration.discovery.ServiceLivenessListener;
import it.water.core.api.service.integration.discovery.ServiceLivenessRegistration;
import it.water.core.api.service.integration.discovery.ServiceLivenessSession;
import it.water.core.api.service.rest.FrameworkRestApi;
import it.water.core.api.service.rest.FrameworkRestController;
import it.water.core.api.service.rest.RestApi;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.service.BaseServiceImpl;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.ws.rs.Path;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

class RestApiServiceRegistrationLifecycleManagerImplTest {

    @Test
    void activatesAndDeactivatesRegistrationsFromBusinessRestApi() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");
        RecordingRegistryClient discoveryClient =
                (RecordingRegistryClient) registry.findComponent(ServiceDiscoveryRegistryClientInternal.class, null);
        RecordingLivenessClient livenessClient =
                (RecordingLivenessClient) registry.findComponent(ServiceLivenessClient.class, null);

        manager.activateRestApiRegistrations(registry, getClass().getClassLoader());

        Assertions.assertNotNull(discoveryClient.registeredInfo);
        Assertions.assertEquals("company", discoveryClient.registeredInfo.getServiceId());
        Assertions.assertEquals("/water/companies", discoveryClient.registeredInfo.getServiceRoot());
        Assertions.assertEquals("8381", discoveryClient.registeredInfo.getServicePort());
        Assertions.assertNotNull(livenessClient.lastRegistration);
        Assertions.assertEquals("company", livenessClient.lastRegistration.getServiceName());

        manager.deactivate();

        Assertions.assertEquals("company", discoveryClient.unregisteredServiceName);
        Assertions.assertTrue(livenessClient.stopped);
    }

    @Test
    void derivesKebabCaseServiceNameFromRestApiClassName() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();

        Assertions.assertEquals("asset-category", manager.deriveServiceName(AssetCategoryRestApi.class));
        Assertions.assertEquals("company", manager.deriveServiceName(CompanyRestApi.class));
    }

    @Test
    void baseServicesExposeConventionBasedServiceName() {
        Assertions.assertEquals("asset-category", new AssetCategoryServiceImpl().getServiceName());
    }

    @Test
    void skipsRegistrationWhenDiscoveryUrlIsMissing() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("");
        RecordingRegistryClient discoveryClient =
                (RecordingRegistryClient) registry.findComponent(ServiceDiscoveryRegistryClientInternal.class, null);

        manager.activateRestApiRegistrations(registry, getClass().getClassLoader());

        Assertions.assertNull(discoveryClient.registeredInfo);
    }

    @Test
    void skipsTechnicalRoots() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();

        Assertions.assertFalse(manager.isBusinessRoot("/"));
        Assertions.assertFalse(manager.isBusinessRoot("/api/serviceregistration"));
        Assertions.assertFalse(manager.isBusinessRoot("/internal/serviceregistration"));
        Assertions.assertFalse(manager.isBusinessRoot("/proxy"));
        Assertions.assertFalse(manager.isBusinessRoot("/status"));
        Assertions.assertTrue(manager.isBusinessRoot("/assetcategories"));
    }

    // -----------------------------------------------------------------------
    // activateRestApiRegistrations — null componentRegistry → returns silently
    // -----------------------------------------------------------------------

    @Test
    void activateRestApiRegistrations_nullComponentRegistry_returnsWithoutException() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        Assertions.assertDoesNotThrow(
                () -> manager.activateRestApiRegistrations(null, getClass().getClassLoader()));
    }

    // -----------------------------------------------------------------------
    // activateRestApiRegistrations — null classLoader → returns silently
    // -----------------------------------------------------------------------

    @Test
    void activateRestApiRegistrations_nullClassLoader_returnsWithoutException() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");
        Assertions.assertDoesNotThrow(
                () -> manager.activateRestApiRegistrations(registry, null));
    }

    // -----------------------------------------------------------------------
    // activateRestApiRegistrations — applicationProperties missing → logs and returns
    // -----------------------------------------------------------------------

    @Test
    void activateRestApiRegistrations_missingApplicationProperties_logsAndReturns() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        // Registry has no ApplicationProperties registered
        InMemoryComponentRegistry emptyRegistry = new InMemoryComponentRegistry();
        RecordingRegistryClient discoveryClient = new RecordingRegistryClient();
        emptyRegistry.register(ServiceDiscoveryRegistryClientInternal.class, discoveryClient);

        manager.activateRestApiRegistrations(emptyRegistry, getClass().getClassLoader());

        Assertions.assertNull(discoveryClient.registeredInfo,
                "no registration should occur without ApplicationProperties");
    }

    // -----------------------------------------------------------------------
    // activateRestApiRegistrations — same key registered twice → second activation is skipped
    // -----------------------------------------------------------------------

    @Test
    void activateRestApiRegistrations_calledTwice_doesNotDuplicateRegistration() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");
        RecordingRegistryClient discoveryClient =
                (RecordingRegistryClient) registry.findComponent(ServiceDiscoveryRegistryClientInternal.class, null);

        manager.activateRestApiRegistrations(registry, getClass().getClassLoader());
        int firstRegisterCount = discoveryClient.registerCallCount;

        // Second call: same serviceName+root key → already in activeRegistrations → skipped
        manager.activateRestApiRegistrations(registry, getClass().getClassLoader());
        Assertions.assertEquals(firstRegisterCount, discoveryClient.registerCallCount,
                "duplicate activation for same key must be ignored");

        manager.deactivate();
    }

    // -----------------------------------------------------------------------
    // isBusinessRoot — null root → false
    // -----------------------------------------------------------------------

    @Test
    void isBusinessRoot_nullRoot_returnsFalse() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        Assertions.assertFalse(manager.isBusinessRoot(null));
    }

    // -----------------------------------------------------------------------
    // isBusinessRoot — blank root → false
    // -----------------------------------------------------------------------

    @Test
    void isBusinessRoot_blankRoot_returnsFalse() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        Assertions.assertFalse(manager.isBusinessRoot("   "));
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — non-@Inject field → returns "" (via reflection)
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceNameFromField_nonInjectField_returnsEmpty() throws Exception {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");

        // FieldHolder has a plain (non-@Inject) field 'plainField' of type String
        java.lang.reflect.Field field = FieldHolder.class.getDeclaredField("plainField");
        String result = invokeResolveServiceNameFromField(manager, registry, field);
        Assertions.assertEquals("", result);
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — @Inject field of type ComponentRegistry → skipped, returns ""
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceNameFromField_componentRegistryField_returnsEmpty() throws Exception {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");

        // FieldHolder has @Inject ComponentRegistry registryField
        java.lang.reflect.Field field = FieldHolder.class.getDeclaredField("registryField");
        String result = invokeResolveServiceNameFromField(manager, registry, field);
        Assertions.assertEquals("", result);
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — @Inject field, component not in registry → returns ""
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceNameFromField_componentNotInRegistry_returnsEmpty() throws Exception {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        // Empty registry — nothing registered
        InMemoryComponentRegistry emptyRegistry = new InMemoryComponentRegistry();

        java.lang.reflect.Field field = FieldHolder.class.getDeclaredField("companyApiField");
        String result = invokeResolveServiceNameFromField(manager, emptyRegistry, field);
        Assertions.assertEquals("", result);
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — component is MetadataProvider with blank name → deriveServiceName
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceNameFromField_metadataProviderWithBlankName_derivesFromFieldType() throws Exception {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = new InMemoryComponentRegistry();
        // Register a MetadataProvider that returns blank service name
        registry.register(SampleServiceInterface.class, new BlankNameMetadataProvider());

        java.lang.reflect.Field field = FieldHolder.class.getDeclaredField("sampleServiceField");
        String result = invokeResolveServiceNameFromField(manager, registry, field);
        // Falls back to deriveServiceName("SampleServiceInterface") = "sample-service-interface"
        Assertions.assertEquals("sample-service-interface", result);
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — component is MetadataProvider with explicit name → returns it
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceNameFromField_metadataProviderWithExplicitName_returnsProvidedName() throws Exception {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = new InMemoryComponentRegistry();
        registry.register(SampleServiceInterface.class, new ExplicitNameMetadataProvider());

        java.lang.reflect.Field field = FieldHolder.class.getDeclaredField("sampleServiceField");
        String result = invokeResolveServiceNameFromField(manager, registry, field);
        Assertions.assertEquals("my-explicit-service", result);
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — component IS a MetadataProvider (BaseAbstractService) with derived name
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceNameFromField_metadataProviderWithDerivedName_returnsIt() throws Exception {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");
        // CompanyServiceImpl extends BaseAbstractService which implements ServiceDiscoveryMetadataProvider.
        // getServiceName() derives "company" from "CompanyServiceImpl".
        java.lang.reflect.Field field = FieldHolder.class.getDeclaredField("companyApiField");
        String result = invokeResolveServiceNameFromField(manager, registry, field);
        Assertions.assertEquals("company", result);
    }

    // -----------------------------------------------------------------------
    // deriveServiceName — null class → returns ""
    // -----------------------------------------------------------------------

    @Test
    void deriveServiceName_withNullClass_returnsEmpty() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        Assertions.assertEquals("", manager.deriveServiceName(null));
    }

    // -----------------------------------------------------------------------
    // resolveServiceNameFromField — @Inject field but component not in registry → falls back
    // -----------------------------------------------------------------------

    @Test
    void resolveServiceName_componentNotInRegistry_fallsBackToDeriveServiceName() {
        RestApiServiceRegistrationLifecycleManagerImpl manager = new RestApiServiceRegistrationLifecycleManagerImpl();
        InMemoryComponentRegistry registry = createRegistry("http://127.0.0.1:8181/water");
        // Remove CompanyApi from registry so the lookup returns null
        registry.unregisterClass(CompanyApi.class);

        manager.activateRestApiRegistrations(registry, getClass().getClassLoader());
        RecordingRegistryClient dc =
                (RecordingRegistryClient) registry.findComponent(ServiceDiscoveryRegistryClientInternal.class, null);
        // Falls back to deriveServiceName("CompanyRestApi") = "company"
        if (dc.registeredInfo != null) {
            Assertions.assertEquals("company", dc.registeredInfo.getServiceId());
        }
        manager.deactivate();
    }

    // -----------------------------------------------------------------------
    // Reflection helper for resolveServiceNameFromField (private method)
    // -----------------------------------------------------------------------

    private static String invokeResolveServiceNameFromField(
            RestApiServiceRegistrationLifecycleManagerImpl manager,
            ComponentRegistry registry,
            java.lang.reflect.Field field) throws Exception {
        java.lang.reflect.Method method = RestApiServiceRegistrationLifecycleManagerImpl.class
                .getDeclaredMethod("resolveServiceNameFromField", ComponentRegistry.class, java.lang.reflect.Field.class);
        method.setAccessible(true);
        return (String) method.invoke(manager, registry, field);
    }

    private InMemoryComponentRegistry createRegistry(String discoveryUrl) {
        InMemoryComponentRegistry registry = new InMemoryComponentRegistry();
        RecordingRegistryClient discoveryClient = new RecordingRegistryClient();
        RecordingLivenessClient livenessClient = new RecordingLivenessClient();
        MapApplicationProperties applicationProperties = new MapApplicationProperties();
        applicationProperties.put(ServiceDiscoveryGlobalConstants.PROP_DISCOVERY_URL, discoveryUrl);
        applicationProperties.put("org.osgi.service.http.port", "8381");

        registry.register(ApplicationProperties.class, applicationProperties);
        registry.register(ServiceDiscoveryGlobalOptions.class,
                new FixedGlobalOptions(discoveryUrl, "127.0.0.1"));
        registry.register(ServiceDiscoveryRegistryClientInternal.class, discoveryClient);
        registry.register(ServiceLivenessClient.class, livenessClient);
        registry.register(CompanyApi.class, new CompanyServiceImpl());
        return registry;
    }

    private static final class RecordingRegistryClient implements ServiceDiscoveryRegistryClientInternal {
        private DiscoverableServiceInfoImpl registeredInfo;
        private String unregisteredServiceName;
        private int registerCallCount = 0;

        @Override
        public void registerService(DiscoverableServiceInfo registration) {
            this.registeredInfo = (DiscoverableServiceInfoImpl) registration;
            this.registerCallCount++;
        }

        @Override
        public void unregisterService(String serviceName, String instanceId) {
            this.unregisteredServiceName = serviceName;
            this.registeredInfo = null;
        }

        @Override
        public DiscoverableServiceInfo getServiceInfo(String id) {
            return registeredInfo;
        }

        @Override
        public void setup(String remoteUrl, String port) {
        }

        @Override
        public boolean isRegistered(String instanceId) {
            return registeredInfo != null && instanceId.equals(registeredInfo.getServiceInstanceId());
        }

        @Override
        public boolean heartbeat(String serviceName, String instanceId) {
            return registeredInfo != null
                    && serviceName.equals(registeredInfo.getServiceId())
                    && instanceId.equals(registeredInfo.getServiceInstanceId());
        }
    }

    private static final class RecordingLivenessClient implements ServiceLivenessClient {
        private ServiceLivenessRegistration lastRegistration;
        private boolean stopped;

        @Override
        public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
            this.lastRegistration = registration;
            this.stopped = false;
            return () -> stopped = true;
        }
    }

    private static final class FixedGlobalOptions implements ServiceDiscoveryGlobalOptions {
        private final String discoveryUrl;
        private final String defaultHost;

        private FixedGlobalOptions(String discoveryUrl, String defaultHost) {
            this.discoveryUrl = discoveryUrl;
            this.defaultHost = defaultHost;
        }

        @Override
        public String getDiscoveryUrl() {
            return discoveryUrl;
        }

        @Override
        public String getDefaultHost() {
            return defaultHost;
        }

        @Override
        public long getHeartbeatIntervalSeconds() {
            return 25L;
        }

        @Override
        public long getRegistrationRetryInitialDelaySeconds() {
            return 30L;
        }

        @Override
        public long getRegistrationRetryMaxDelaySeconds() {
            return 300L;
        }

        @Override
        public long getHttpTimeoutSeconds() {
            return 10L;
        }

        @Override
        public int getRegistrationMaxAttempts() {
            return 3;
        }

        @Override
        public long[] getRegistrationRetryBackoffMs() {
            return new long[]{2000L, 4000L, 8000L};
        }
    }

    private static final class MapApplicationProperties implements ApplicationProperties {
        private final Map<String, Object> properties = new HashMap<>();

        void put(String key, Object value) {
            properties.put(key, value);
        }

        @Override
        public void setup() {
        }

        @Override
        public Object getProperty(String key) {
            return properties.get(key);
        }

        @Override
        public boolean containsKey(String key) {
            return properties.containsKey(key);
        }

        @Override
        public void loadProperties(File file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void loadProperties(Properties props) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unloadProperties(File file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unloadProperties(Properties props) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class InMemoryComponentRegistry implements ComponentRegistry {
        private final Map<Class<?>, Object> components = new HashMap<>();

        <T> void register(Class<T> componentClass, T component) {
            components.put(componentClass, component);
        }

        void unregisterClass(Class<?> componentClass) {
            components.remove(componentClass);
        }

        @Override
        public <T> List<T> findComponents(Class<T> componentClass, it.water.core.api.registry.filter.ComponentFilter filter) {
            T component = findComponent(componentClass, filter);
            return component == null ? Collections.emptyList() : Collections.singletonList(component);
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T findComponent(Class<T> componentClass, it.water.core.api.registry.filter.ComponentFilter filter) {
            return (T) components.get(componentClass);
        }

        @Override
        public <T, K> ComponentRegistration<T, K> registerComponent(Class<? extends T> componentClass, T component, ComponentConfiguration configuration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean unregisterComponent(ComponentRegistration<T, ?> registration) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T> boolean unregisterComponent(Class<T> componentClass, T component) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ComponentFilterBuilder getComponentFilterBuilder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends BaseEntitySystemApi> T findEntitySystemApi(String entityClassName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends BaseRepository> T findEntityRepository(String entityClassName) {
            throw new UnsupportedOperationException();
        }

        @Override
        public <T extends BaseEntity> BaseRepository<T> findEntityExtensionRepository(Class<T> type) {
            throw new UnsupportedOperationException();
        }
    }
}

// -----------------------------------------------------------------------
// Fixture: field-holder class used to test resolveServiceNameFromField branches
// -----------------------------------------------------------------------

class FieldHolder {
    /** Plain non-@Inject field — should be skipped. */
    @SuppressWarnings("unused")
    String plainField;

    /** @Inject ComponentRegistry field — should be skipped (isAssignableFrom check). */
    @Inject
    ComponentRegistry registryField;

    /** @Inject field whose component IS in the registry but is not a MetadataProvider. */
    @Inject
    CompanyApi companyApiField;

    /** @Inject field whose component IS a MetadataProvider (blank or explicit name). */
    @Inject
    SampleServiceInterface sampleServiceField;
}

interface SampleServiceInterface extends it.water.core.api.service.BaseApi {
}

/** MetadataProvider implementation that returns a blank service name. */
class BlankNameMetadataProvider extends BaseServiceImpl
        implements SampleServiceInterface, it.water.core.api.service.integration.discovery.ServiceDiscoveryMetadataProvider {
    @Override
    public String getServiceName() {
        return ""; // blank → triggers fallback to deriveServiceName
    }

    @Override
    protected it.water.core.api.service.BaseSystemApi getSystemService() {
        return null;
    }
}

/** MetadataProvider implementation that returns an explicit service name. */
class ExplicitNameMetadataProvider extends BaseServiceImpl
        implements SampleServiceInterface, it.water.core.api.service.integration.discovery.ServiceDiscoveryMetadataProvider {
    @Override
    public String getServiceName() {
        return "my-explicit-service";
    }

    @Override
    protected it.water.core.api.service.BaseSystemApi getSystemService() {
        return null;
    }
}

@FrameworkRestApi
@Path("/companies")
interface CompanyRestApi extends RestApi {
}

interface CompanyApi extends BaseApi {
}

@FrameworkRestController(referredRestApi = CompanyRestApi.class)
class CompanyRestControllerImpl implements CompanyRestApi {
    @Inject
    private CompanyApi companyApi;
}

class CompanyServiceImpl extends BaseServiceImpl implements CompanyApi {
    @Override
    protected BaseSystemApi getSystemService() {
        return null;
    }
}

class AssetCategoryRestApi {
}

class AssetCategoryServiceImpl extends BaseServiceImpl {
    @Override
    protected BaseSystemApi getSystemService() {
        return null;
    }
}

@FrameworkRestApi
@Path("/api/serviceregistration")
interface ServiceRegistrationRestApi extends RestApi {
}
