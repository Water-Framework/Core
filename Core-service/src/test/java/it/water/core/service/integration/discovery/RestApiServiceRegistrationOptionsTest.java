package it.water.core.service.integration.discovery;

import it.water.core.api.bundle.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

class RestApiServiceRegistrationOptionsTest {

    @Test
    void resolvesStaticIdentityFromRestApiAndRuntimePortFromProperties() {
        MapApplicationProperties applicationProperties = new MapApplicationProperties();
        applicationProperties.put("org.osgi.service.http.port", "8381");

        RestApiServiceRegistrationOptions options =
                new RestApiServiceRegistrationOptions("asset-category", "/assetcategories", applicationProperties);

        Assertions.assertEquals("", options.getDiscoveryUrl());
        Assertions.assertEquals("asset-category", options.getServiceName());
        Assertions.assertEquals("/water/assetcategories", options.getRoot());
        Assertions.assertEquals("1.0.0", options.getServiceVersion());
        Assertions.assertEquals("http", options.getProtocol());
        Assertions.assertEquals("8381", options.getServicePort());
        Assertions.assertEquals("", options.getAdvertisedEndpoint());
        Assertions.assertEquals("", options.getInstanceId());
        Assertions.assertEquals("", options.getServiceHost());
    }

    @Test
    void fallsBackToSpringPortWhenOsgiPortIsMissing() {
        MapApplicationProperties applicationProperties = new MapApplicationProperties();
        applicationProperties.put("server.port", "9082");
        applicationProperties.put("server.servlet.context-path", "/custom");

        RestApiServiceRegistrationOptions options =
                new RestApiServiceRegistrationOptions("asset-tag", "/assettags", applicationProperties);

        Assertions.assertEquals("9082", options.getServicePort());
        Assertions.assertEquals("/custom/assettags", options.getRoot());
    }

    private static final class MapApplicationProperties implements ApplicationProperties {
        private final Properties properties = new Properties();

        void put(String key, String value) {
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
            properties.putAll(props);
        }

        @Override
        public void unloadProperties(File file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void unloadProperties(Properties props) {
            props.keySet().forEach(properties::remove);
        }
    }
}
