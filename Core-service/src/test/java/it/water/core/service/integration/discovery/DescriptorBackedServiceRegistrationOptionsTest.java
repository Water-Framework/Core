package it.water.core.service.integration.discovery;

import it.water.core.api.bundle.ApplicationProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Properties;

class DescriptorBackedServiceRegistrationOptionsTest {

    @Test
    void resolvesStaticIdentityFromDescriptorAndRuntimePortFromProperties() {
        WaterServiceRegistrationDescriptor descriptor = new WaterServiceRegistrationDescriptor(
                "it.water.assetcategory.service",
                "asset-category",
                "/water/assetcategories",
                "3.0.0",
                "http");
        MapApplicationProperties applicationProperties = new MapApplicationProperties();
        applicationProperties.put("water.discovery.url", "http://127.0.0.1:8181/water");
        applicationProperties.put("org.osgi.service.http.port", "8381");

        DescriptorBackedServiceRegistrationOptions options =
                new DescriptorBackedServiceRegistrationOptions(descriptor, applicationProperties);

        Assertions.assertEquals("http://127.0.0.1:8181/water", options.getDiscoveryUrl());
        Assertions.assertEquals("asset-category", options.getServiceName());
        Assertions.assertEquals("/water/assetcategories", options.getRoot());
        Assertions.assertEquals("3.0.0", options.getServiceVersion());
        Assertions.assertEquals("http", options.getProtocol());
        Assertions.assertEquals("8381", options.getServicePort());
        Assertions.assertEquals("", options.getAdvertisedEndpoint());
        Assertions.assertEquals("", options.getInstanceId());
        Assertions.assertEquals("", options.getServiceHost());
    }

    @Test
    void fallsBackToSpringPortWhenOsgiPortIsMissing() {
        WaterServiceRegistrationDescriptor descriptor = new WaterServiceRegistrationDescriptor(
                "it.water.assettag.service",
                "asset-tag",
                "/water/assettags",
                "3.0.0",
                "http");
        MapApplicationProperties applicationProperties = new MapApplicationProperties();
        applicationProperties.put("server.port", "9082");

        DescriptorBackedServiceRegistrationOptions options =
                new DescriptorBackedServiceRegistrationOptions(descriptor, applicationProperties);

        Assertions.assertEquals("9082", options.getServicePort());
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
