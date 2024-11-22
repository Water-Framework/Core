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

package it.water.core.testing.utils.junit;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.interceptors.WaterAbstractInterceptor;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.testing.utils.bundle.TestRuntimeInitializer;
import org.apache.cxf.transport.servlet.CXFServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.lang.reflect.Method;

public class WaterTestExtension extends WaterAbstractInterceptor<Service> implements Extension, BeforeEachCallback, BeforeAllCallback {

    @Override
    protected ComponentRegistry getComponentsRegistry() {
        return TestRuntimeInitializer.getInstance().getComponentRegistry();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        startJetty();
        TestRuntimeInitializer.getInstance().start();
        Method m = findBeforeAllMethod(extensionContext.getRequiredTestClass());
        if (m != null) {
            //forcing test class and method to be visible
            m.setAccessible(true);
            this.executeInterceptorBeforeMethod((Service) extensionContext.getRequiredTestInstance(), m, m.getParameters());
        }
    }

    private Method findBeforeAllMethod(Class<?> testClass) {
        Method[] methods = testClass.getDeclaredMethods();

        for (Method method : methods) {
            if (method.isAnnotationPresent(BeforeAll.class)) {
                return method;
            }
        }

        return null;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
        if (!(extensionContext.getRequiredTestInstance() instanceof Service))
            throw new WaterRuntimeException("Please implement it.water.core.api.service.Service interface in your test class");
        Method m = extensionContext.getRequiredTestMethod();
        //forcing test class and method to be visible
        extensionContext.getRequiredTestClass().getDeclaredMethod(m.getName(), m.getParameterTypes()).setAccessible(true);
        this.executeInterceptorBeforeMethod((Service) extensionContext.getRequiredTestInstance(), m, m.getParameters());
    }

    private void startJetty() throws Exception {
        if (TestRuntimeInitializer.getInstance().hasRestApi() && !TestRuntimeInitializer.getInstance().isServerStarted()) {
            Server server = new Server();
            ServerConnector connector = new ServerConnector(server);
            connector.setPort(0);
            server.addConnector(connector);
            // Register and map the dispatcher servlet
            final ServletHolder servletHolder = new ServletHolder(new CXFServlet());
            final ServletContextHandler context = new ServletContextHandler();
            context.setContextPath("/");
            context.addServlet(servletHolder, "/water/*");
            server.setHandler(context);
            server.start();
            TestRuntimeInitializer.getInstance().setRestServerPort(String.valueOf(connector.getLocalPort()));
            TestRuntimeInitializer.getInstance().setServerStarted(true);
        }
    }

}
