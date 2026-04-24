package it.water.core.service.integration.discovery;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

class WaterModuleDescriptorLoaderTest {

    @Test
    void parsesServiceRegistrationSectionFromWaterDescriptor() throws Exception {
        String descriptor = "{\n" +
                "  \"schemaVersion\": \"1.1\",\n" +
                "  \"moduleId\": \"it.water.assetcategory.service\",\n" +
                "  \"runtime\": {\n" +
                "    \"serviceRegistration\": {\n" +
                "      \"enabled\": true,\n" +
                "      \"serviceName\": \"asset-category\",\n" +
                "      \"root\": \"/water/assetcategories\",\n" +
                "      \"serviceVersion\": \"3.0.0\",\n" +
                "      \"protocol\": \"http\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        WaterServiceRegistrationDescriptor parsed = WaterModuleDescriptorLoader.parse(
                new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNotNull(parsed);
        Assertions.assertEquals("it.water.assetcategory.service", parsed.getModuleId());
        Assertions.assertEquals("asset-category", parsed.getServiceName());
        Assertions.assertEquals("/water/assetcategories", parsed.getRoot());
        Assertions.assertEquals("3.0.0", parsed.getServiceVersion());
        Assertions.assertEquals("http", parsed.getProtocol());
    }

    @Test
    void ignoresDescriptorsWithoutEnabledServiceRegistration() throws Exception {
        String descriptor = "{\n" +
                "  \"schemaVersion\": \"1.1\",\n" +
                "  \"moduleId\": \"it.water.assetcategory.service\",\n" +
                "  \"runtime\": {\n" +
                "    \"serviceRegistration\": {\n" +
                "      \"enabled\": false,\n" +
                "      \"serviceName\": \"asset-category\",\n" +
                "      \"root\": \"/water/assetcategories\"\n" +
                "    }\n" +
                "  }\n" +
                "}";

        WaterServiceRegistrationDescriptor parsed = WaterModuleDescriptorLoader.parse(
                new ByteArrayInputStream(descriptor.getBytes(StandardCharsets.UTF_8)));

        Assertions.assertNull(parsed);
    }
}
