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
package it.water.core.service.traffic.interceptors;

import it.water.core.api.bundle.ApplicationProperties;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.repository.BaseRepository;
import it.water.core.api.service.BaseApi;
import it.water.core.api.service.BaseSystemApi;
import it.water.core.api.service.Service;
import it.water.core.api.service.rest.RestApi;
import it.water.core.api.traffic.TrafficReporter;
import it.water.core.api.traffic.annotations.NoTraffic;
import it.water.core.api.traffic.model.TrafficCallRecord;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pins down the SCOPE of the S2S capture: only methods declared by one of Water's four architectural
 * layers - {@link RestApi}, {@link BaseApi}, {@link BaseSystemApi}, {@link BaseRepository} - are
 * reported, and everything else is ignored.
 * <p>
 * This is the guard against the scope silently widening again. An earlier iteration captured every
 * proxied method and needed two extra mechanisms to stay usable (an infrastructure exclusion list and
 * a re-entrancy guard), because Water's interceptors and the traffic pipeline are themselves
 * {@code Service}s and were being captured while the chain worked. Both mechanisms are gone: the tests
 * below show the layer scope alone keeps them out.
 * <p>
 * Driven directly through the two {@code interceptMethod} hooks with Mockito, so each scope decision
 * is exercised in isolation - the end-to-end wiring is covered by {@code S2STrafficCaptureHarnessTest}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TrafficS2SInterceptorScopeTest {

    @Mock
    private ComponentRegistry componentRegistry;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private TrafficReporter trafficReporter;

    private TrafficS2SInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TrafficS2SInterceptor();
        interceptor.setComponentsRegistry(componentRegistry);
        interceptor.setApplicationProperties(applicationProperties);
        when(applicationProperties.getPropertyOrDefault("water.traffic.s2s.enabled", "false")).thenReturn("true");
        when(applicationProperties.getPropertyOrDefault("water.traffic.s2s.exclude", "")).thenReturn("");
        when(componentRegistry.findComponents(TrafficReporter.class, null)).thenReturn(List.of(trafficReporter));
        when(trafficReporter.isEnabled()).thenReturn(true);
    }

    @AfterEach
    void drainSpans() {
        // the before-hook pushes onto a ThreadLocal deque; JUnit reuses this thread across methods
        while (TrafficCallContext.pop() != null) {
            // draining
        }
    }

    /**
     * Runs a full before/after pair, the way the framework would around a real invocation.
     */
    private <S extends Service> void invoke(S target, Class<?> declaringInterface, String methodName) throws NoSuchMethodException {
        Method m = declaringInterface.getMethod(methodName);
        interceptor.interceptMethod(target, m, new Object[0]);
        interceptor.interceptMethod(target, m, new Object[0], "result");
    }

    private TrafficCallRecord capturedRecord() {
        ArgumentCaptor<TrafficCallRecord> captor = ArgumentCaptor.forClass(TrafficCallRecord.class);
        verify(trafficReporter).reportCall(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------------
    // The four traced layers
    // ---------------------------------------------------------------------

    @Test
    void restApiMethod_isCaptured() throws NoSuchMethodException {
        invoke(new TracedRestApiImpl(), TracedRestApi.class, "restWork");

        assertEquals("restWork", capturedRecord().operation());
    }

    @Test
    void apiMethod_isCaptured() throws NoSuchMethodException {
        invoke(new TracedApiImpl(), TracedApi.class, "apiWork");

        assertEquals("apiWork", capturedRecord().operation());
    }

    @Test
    void systemApiMethod_isCaptured() throws NoSuchMethodException {
        invoke(new TracedSystemApiImpl(), TracedSystemApi.class, "systemWork");

        assertNotNull(capturedRecord());
    }

    @Test
    void repositoryMethod_isCaptured() throws NoSuchMethodException {
        invoke(mock(TracedRepository.class), TracedRepository.class, "repositoryWork");

        assertEquals("repositoryWork", capturedRecord().operation());
    }

    /**
     * A method inherited from {@link BaseRepository} itself, not redeclared on the concrete repository
     * interface: the traced layer is the one that DECLARES the method, so walking up the interface
     * hierarchy is what makes the persistence hop visible.
     */
    @Test
    void methodDeclaredOnBaseRepositoryItself_isCaptured() throws NoSuchMethodException {
        Method removeAll = BaseRepository.class.getMethod("removeAll");
        interceptor.interceptMethod(mock(TracedRepository.class), removeAll, new Object[0]);
        interceptor.interceptMethod(mock(TracedRepository.class), removeAll, new Object[0], null);

        assertEquals("removeAll", capturedRecord().operation());
    }

    // ---------------------------------------------------------------------
    // Everything else is out of scope
    // ---------------------------------------------------------------------

    @Test
    void plainServiceMethod_isIgnored() throws NoSuchMethodException {
        invoke(new PlainServiceImpl(), PlainService.class, "internalWork");

        verify(trafficReporter, never()).reportCall(any());
    }

    /**
     * The reason the infrastructure exclusion list could be deleted: a component's own extra interface
     * is not a layer, so its methods are not traffic even though the component IS on a traced layer.
     */
    @Test
    void methodFromANonLayerInterfaceOfATracedComponent_isIgnored() throws NoSuchMethodException {
        invoke(new TracedApiImpl(), PlainService.class, "internalWork");

        verify(trafficReporter, never()).reportCall(any());
    }

    @Test
    void noTrafficAnnotatedLayerMethod_isExcluded() throws NoSuchMethodException {
        invoke(new TracedApiImpl(), TracedApi.class, "silentApiWork");

        verify(trafficReporter, never()).reportCall(any());
    }

    @Test
    void excludedByProperty_isNotCaptured() throws NoSuchMethodException {
        when(applicationProperties.getPropertyOrDefault("water.traffic.s2s.exclude", ""))
                .thenReturn(TracedApiImpl.class.getName() + "#apiWork");

        invoke(new TracedApiImpl(), TracedApi.class, "apiWork");

        verify(trafficReporter, never()).reportCall(any());
    }

    @Test
    void captureDisabledByProperty_layerMethodIsNotCaptured() throws NoSuchMethodException {
        when(applicationProperties.getPropertyOrDefault("water.traffic.s2s.enabled", "false")).thenReturn("false");

        invoke(new TracedApiImpl(), TracedApi.class, "apiWork");

        verify(trafficReporter, never()).reportCall(any());
    }

    // ---------------------------------------------------------------------
    // Fixtures: one per layer, plus a plain Service
    // ---------------------------------------------------------------------

    interface TracedRestApi extends RestApi {
        String restWork();
    }

    interface TracedApi extends BaseApi {
        String apiWork();

        @NoTraffic
        String silentApiWork();
    }

    interface TracedSystemApi extends BaseSystemApi {
        String systemWork();
    }

    interface TracedRepository extends BaseRepository<DummyEntity> {
        String repositoryWork();
    }

    interface PlainService extends Service {
        String internalWork();
    }

    static class TracedRestApiImpl implements TracedRestApi {
        @Override
        public String restWork() {
            return "rest";
        }
    }

    /**
     * Implements a traced layer AND a plain one, so the "scope is per method, not per component" rule
     * can be exercised on a single instance.
     */
    static class TracedApiImpl implements TracedApi, PlainService {
        @Override
        public String apiWork() {
            return "api";
        }

        @Override
        public String silentApiWork() {
            return "silent";
        }

        @Override
        public String internalWork() {
            return "internal";
        }
    }

    static class TracedSystemApiImpl implements TracedSystemApi {
        @Override
        public String systemWork() {
            return "system";
        }
    }

    static class PlainServiceImpl implements PlainService {
        @Override
        public String internalWork() {
            return "internal";
        }
    }

    /**
     * Minimal {@link BaseEntity} needed only to parameterize {@link BaseRepository}.
     */
    interface DummyEntity extends BaseEntity {
    }
}
