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
package it.water.core.registry;


import it.water.core.api.registry.ComponentConfiguration;
import it.water.core.api.registry.filter.ComponentFilter;
import it.water.core.model.exceptions.WaterException;
import it.water.core.registry.filter.ComponentDefaultPropertyFilter;
import it.water.core.registry.model.PropertiesComponentConfiguration;
import it.water.core.registry.model.ComponentConfigurationFactory;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;

class WaterBaseRegistryTest {
    private PropertiesComponentConfiguration config;

    @BeforeEach
    void setUp() {
        config = new PropertiesComponentConfiguration();
    }

    @Test
    void testComponentBasicFilter() {
        TestComponentFilterBuilder filterBuilder = new TestComponentFilterBuilder();
        String filter = filterBuilder.createFilter("field1", "value1").and("field2", "value2").or("field3", "value3").getFilter();
        Assertions.assertEquals("(|(&(field1=value1)(field2=value2))(field3=value3))", filter);
    }

    @Test
    void testWaterComponentNotFoundException() {
        NoComponentRegistryFoundException ex = new NoComponentRegistryFoundException();
        Assertions.assertNotNull(ex);
        ex = new NoComponentRegistryFoundException("message");
        Assertions.assertEquals("message", ex.getMessage());
        ex = new NoComponentRegistryFoundException("message", new WaterException());
        Assertions.assertEquals("message", ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
        ex = new NoComponentRegistryFoundException("message", new WaterException(),false,false);
        Assertions.assertEquals("message", ex.getMessage());
        Assertions.assertNotNull(ex.getCause());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testComponentPropertiesFactory() {
        ComponentConfigurationFactory<Object> factory = new ComponentConfigurationFactory<>();
        @SuppressWarnings("rawtypes")
        Dictionary props = new Hashtable();
        props.put("newProp", "propValue");
        props.put("newProp1", "propValue1");
        @SuppressWarnings({ "rawtypes" })
        ComponentConfigurationFactory conf = factory.fromGenericDictionary(props);
        Assertions.assertNotNull(conf);
        Assertions.assertTrue(conf.build().hasProperty("newProp"));
        Assertions.assertTrue(conf.build().hasProperty("newProp1"));
    }

    @Test
    void testGetConfiguration() {
        // Verify that the returned Properties object is not the same object as the internal props
        Properties configuration = config.getConfiguration();
        Assertions.assertNotSame(configuration, config.getConfiguration());
    }

    @Test
    void testGetConfigurationAsDictionary() {
        // Verify that the returned Dictionary object is an instance of Hashtable
        Dictionary<String, Object> dictionary = config.getConfigurationAsDictionary();
        Assertions.assertTrue(dictionary instanceof Hashtable);
    }

    @Test
    void testAddProperty() {
        // Verify that a property can be added to the configuration
        config.addProperty("key", "value");
        config.addProperty("key", "value");
        Assertions.assertTrue(config.hasProperty("key"));
    }

    @Test
    void testRemoveProperty() {
        // Verify that a property can be removed from the configuration
        config.addProperty("key", "value");
        config.removeProperty("key");
        Assertions.assertFalse(config.hasProperty("key"));
        config.removeProperty("notExists");
    }

    @Test
    void testHasProperty() {
        // Verify that hasProperty() returns true when a property exists and false otherwise
        config.addProperty("key", "value");
        Assertions.assertTrue(config.hasProperty("key"));
        Assertions.assertFalse(config.hasProperty("non-existent-key"));
    }

    @Test
    void testConstructorWithFile() throws IOException {
        // Verify that a properties file can be loaded into the configuration
        File file = new File("src/test/resources/test.properties");
        PropertiesComponentConfiguration configFromFile = new PropertiesComponentConfiguration(file);
        Assertions.assertEquals("value", configFromFile.getConfiguration().getProperty("key"));
    }

    @Test
    void testCreateFromDictionary() {
        // create a dictionary with some properties
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put("foo", "bar");
        dictionary.put("baz", 123);
        // create the configuration using the factory
        ComponentConfiguration tempConfig = ComponentConfigurationFactory
                .createNewComponentPropertyFactory()
                .fromStringDictionary(dictionary)
                .build();
        // check that the properties are set correctly
        Assertions.assertTrue(tempConfig.hasProperty("foo"));
        Assertions.assertEquals("bar", tempConfig.getConfiguration().getProperty("foo"));
        Assertions.assertTrue(tempConfig.hasProperty("baz"));
        Assertions.assertEquals(123, tempConfig.getConfiguration().get("baz"));
    }

    @Test
    void testCreateFromGenericDictionary() {
        // create a generic dictionary with some properties
        Dictionary<Object, Object> dictionary = new Hashtable<>();
        dictionary.put("foo", "bar");
        dictionary.put("42", "answer");
        // create the configuration using the factory
        ComponentConfiguration tempConfig = ComponentConfigurationFactory
                .createNewComponentPropertyFactory()
                .fromGenericDictionary(dictionary)
                .build();
        // check that the properties are set correctly
        Assertions.assertTrue(tempConfig.hasProperty("foo"));
        Assertions.assertEquals("bar", tempConfig.getConfiguration().getProperty("foo"));
        Assertions.assertTrue(tempConfig.hasProperty("42"));
        Assertions.assertEquals("answer", tempConfig.getConfiguration().getProperty("42"));
    }

    @Test
    void testCreateFromMultipleSources() {
        // create a dictionary with some properties
        Dictionary<String, Object> dict1 = new Hashtable<>();
        dict1.put("foo", "bar");
        dict1.put("baz", 123);
        // create another dictionary with some properties
        Dictionary<Object, Object> dict2 = new Hashtable<>();
        dict2.put("qux", "quux");
        dict2.put("corge", 42);
        // create the configuration using the factory
        ComponentConfiguration tempConfig = ComponentConfigurationFactory
                .createNewComponentPropertyFactory()
                .fromStringDictionary(dict1)
                .fromGenericDictionary(dict2)
                .withProp("grault", "garply")
                .build();
        // check that the properties are set correctly
        Assertions.assertTrue(tempConfig.hasProperty("foo"));
        Assertions.assertEquals("bar", tempConfig.getConfiguration().getProperty("foo"));
        Assertions.assertTrue(tempConfig.hasProperty("baz"));
        Assertions.assertEquals(123, tempConfig.getConfiguration().get("baz"));
        Assertions.assertTrue(tempConfig.hasProperty("qux"));
        Assertions.assertEquals("quux", tempConfig.getConfiguration().getProperty("qux"));
        Assertions.assertTrue(tempConfig.hasProperty("corge"));
        Assertions.assertEquals(42, tempConfig.getConfiguration().get("corge"));
        Assertions.assertTrue(tempConfig.hasProperty("grault"));
        Assertions.assertEquals("garply", tempConfig.getConfiguration().getProperty("grault"));
    }

    @Test
    void testBuild() {
        ComponentConfigurationFactory<String> factory = ComponentConfigurationFactory.createNewComponentPropertyFactory();
        ComponentConfiguration configuration = factory.withProp("prop1", "value1")
                .withProp("prop2", 123)
                .build();
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put("prop1", "value1");
        dictionary.put("prop2", 123);
        Assertions.assertEquals(dictionary, configuration.getConfigurationAsDictionary());
    }

    @Test
    void testFromStringDictionary() {
        Dictionary<String, Object> dictionary = new Hashtable<>();
        dictionary.put("prop1", "value1");
        dictionary.put("prop2", 123);
        ComponentConfigurationFactory<String> factory = ComponentConfigurationFactory.createNewComponentPropertyFactory();
        ComponentConfiguration configuration = factory.fromStringDictionary(dictionary)
                .build();
        Assertions.assertEquals(dictionary, configuration.getConfigurationAsDictionary());
    }

    @Test
    void testFromGenericDictionary() {
        Dictionary<Object, Object> dictionary = new Hashtable<>();
        dictionary.put("prop1", "value1");
        dictionary.put("prop2", 123);
        ComponentConfigurationFactory<String> factory = ComponentConfigurationFactory.createNewComponentPropertyFactory();
        factory.withPriority(2);
        ComponentConfiguration configuration = factory.fromGenericDictionary(dictionary)
                .build();
        Dictionary<String, Object> expectedDictionary = new Hashtable<>();
        expectedDictionary.put("prop1", "value1");
        expectedDictionary.put("prop2", 123);
        Assertions.assertEquals(expectedDictionary, configuration.getConfigurationAsDictionary());
        Assertions.assertEquals(2, configuration.getPriority());
    }

    @Test
    void testComponentFilterBuilder() {
        TestComponentFilterBuilder testComponentFilterBuilder = new TestComponentFilterBuilder();
        ComponentFilter cf = testComponentFilterBuilder.createFilter("field", "value");
        ComponentDefaultPropertyFilter componentPropertyFilter = (ComponentDefaultPropertyFilter) cf;
        Assertions.assertFalse(componentPropertyFilter.isNot());
        cf = cf.and(testComponentFilterBuilder.createFilter("field2", "value2"));
        Assertions.assertEquals("(&(field=value)(field2=value2))", cf.getFilter());
        Assertions.assertEquals("(!(&(field=value)(field2=value2)))", cf.not().getFilter());
        componentPropertyFilter.setName("newName");
        Assertions.assertEquals("newName", componentPropertyFilter.getName());
        componentPropertyFilter.setValue("newValue");
        Assertions.assertEquals("newValue", componentPropertyFilter.getValue());
        cf = testComponentFilterBuilder.createFilter("field", "value");
        cf = cf.or(testComponentFilterBuilder.createFilter("field3", "value3"));
        Assertions.assertEquals("(|(field=value)(field3=value3))", cf.getFilter());
        Assertions.assertEquals("(!(|(field=value)(field3=value3)))", cf.not().getFilter());
    }

    @Test
    void testComponentFilter() {
        TestComponentFilterBuilder testComponentFilterBuilder = new TestComponentFilterBuilder();
        Properties props = new Properties();
        props.put("b", "value");
        Assertions.assertTrue(testComponentFilterBuilder.createFilter("a", "value").or("b", "value").or("c", "value").matches(props));
        Assertions.assertFalse(testComponentFilterBuilder.createFilter("a", "value").and("b", "value").matches(props));
    }
}
