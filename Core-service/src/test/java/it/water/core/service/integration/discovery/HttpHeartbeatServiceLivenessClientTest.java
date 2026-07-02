/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import it.water.core.api.service.integration.discovery.DiscoverableServiceInfo;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import it.water.core.api.service.integration.discovery.ServiceLivenessListener;
import it.water.core.api.service.integration.discovery.ServiceLivenessRegistration;
import it.water.core.api.service.integration.discovery.ServiceLivenessSession;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Unit tests for {@link HttpHeartbeatServiceLivenessClient} and its inner
 * {@code HttpHeartbeatSession}. All scheduler interaction is exercised via
 * direct method invocation (reflection on private methods) so tests are
 * fully deterministic with no {@link Thread#sleep} dependencies.
 */
class HttpHeartbeatServiceLivenessClientTest {

    private HttpHeartbeatServiceLivenessClient client;

    @BeforeEach
    void setUp() {
        client = new HttpHeartbeatServiceLivenessClient();
    }

    @AfterEach
    void tearDown() {
        client.deactivate();
    }

    // -----------------------------------------------------------------------
    // Helper: build a ServiceLivenessRegistration
    // -----------------------------------------------------------------------

    private static ServiceLivenessRegistration reg(String serviceName, String instanceId) {
        return new ServiceLivenessRegistration(
                serviceName, instanceId, "1.0.0",
                "http", "/water", "http://host:8080/water",
                "host", "8080", "node-1", "core");
    }

    // -----------------------------------------------------------------------
    // Helper: recording liveness listener
    // -----------------------------------------------------------------------

    private static final class RecordingListener implements ServiceLivenessListener {
        final AtomicReference<ServiceLivenessRegistration> lostRegistration = new AtomicReference<>();
        final AtomicReference<String> lostReason = new AtomicReference<>();

        @Override
        public void onLivenessLost(ServiceLivenessRegistration registration, String reason) {
            lostRegistration.set(registration);
            lostReason.set(reason);
        }
    }

    // -----------------------------------------------------------------------
    // Helper: recording discovery client
    // -----------------------------------------------------------------------

    private static final class RecordingDiscoveryClient implements ServiceDiscoveryRegistryClientInternal {
        private final boolean heartbeatResult;
        final AtomicInteger heartbeatCallCount = new AtomicInteger();

        RecordingDiscoveryClient(boolean heartbeatResult) {
            this.heartbeatResult = heartbeatResult;
        }

        @Override
        public boolean heartbeat(String serviceName, String instanceId) {
            heartbeatCallCount.incrementAndGet();
            return heartbeatResult;
        }

        @Override
        public void registerService(DiscoverableServiceInfo reg) {}
        @Override
        public void unregisterService(String serviceName, String instanceId) {}
        @Override
        public DiscoverableServiceInfo getServiceInfo(String id) { return null; }
        @Override
        public void setup(String remoteUrl, String port) {}
        @Override
        public boolean isRegistered(String instanceId) { return false; }
    }

    // -----------------------------------------------------------------------
    // Helper: access the private sessions map via reflection
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Map<String, Object> getSessions() throws Exception {
        Field f = HttpHeartbeatServiceLivenessClient.class.getDeclaredField("sessions");
        f.setAccessible(true);
        return (Map<String, Object>) f.get(client);
    }

    // -----------------------------------------------------------------------
    // Helper: invoke heartbeatTick on the inner session object via reflection
    // -----------------------------------------------------------------------

    private void invokeHeartbeatTick(Object session) throws Exception {
        Method m = session.getClass().getDeclaredMethod("heartbeatTick");
        m.setAccessible(true);
        m.invoke(session);
    }

    // -----------------------------------------------------------------------
    // start() — session is registered and returned
    // -----------------------------------------------------------------------

    @Test
    void start_registersSessionAndReturnsNonNullHandle() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);

        ServiceLivenessRegistration registration = reg("svc", "inst-1");
        ServiceLivenessSession session = client.start(registration, new RecordingListener());

        Assertions.assertNotNull(session, "start() must return a non-null session handle");
        Assertions.assertTrue(getSessions().containsKey("inst-1"), "session must be tracked internally");
    }

    // -----------------------------------------------------------------------
    // start() — second start for same instanceId stops previous session
    // -----------------------------------------------------------------------

    @Test
    void start_replacesExistingSessionForSameInstanceId() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);

        ServiceLivenessRegistration reg = reg("svc", "inst-dup");
        client.start(reg, new RecordingListener());
        client.start(reg, new RecordingListener()); // replaces the first session

        // After replacement only one entry should exist for the instance id
        Assertions.assertEquals(1, getSessions().size());
    }

    // -----------------------------------------------------------------------
    // stop via returned handle — removes session from map
    // -----------------------------------------------------------------------

    @Test
    void sessionStopHandle_removesSessionFromMap() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);

        ServiceLivenessRegistration registration = reg("svc", "inst-stop");
        ServiceLivenessSession handle = client.start(registration, new RecordingListener());

        Assertions.assertTrue(getSessions().containsKey("inst-stop"));
        handle.stop(); // calls the lambda returned by start()
        Assertions.assertFalse(getSessions().containsKey("inst-stop"), "session must be removed on stop");
    }

    // -----------------------------------------------------------------------
    // heartbeatTick — successful heartbeat: no listener call, session stays
    // -----------------------------------------------------------------------

    @Test
    void heartbeatTick_successfulHeartbeat_doesNotNotifyListener() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);

        RecordingListener listener = new RecordingListener();
        ServiceLivenessRegistration registration = reg("svc", "inst-ok");
        client.start(registration, listener);

        // Get the session object from the map
        Object session = getSessions().get("inst-ok");
        Assertions.assertNotNull(session);

        invokeHeartbeatTick(session);

        Assertions.assertNull(listener.lostRegistration.get(), "listener must NOT be called on success");
        Assertions.assertTrue(getSessions().containsKey("inst-ok"), "session must still be registered");
    }

    // -----------------------------------------------------------------------
    // heartbeatTick — failed heartbeat: listener notified, session removed
    // -----------------------------------------------------------------------

    @Test
    void heartbeatTick_failedHeartbeat_notifiesListenerAndRemovesSession() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(false);
        client.setDiscoveryClient(dc);

        RecordingListener listener = new RecordingListener();
        ServiceLivenessRegistration registration = reg("svc", "inst-fail");
        client.start(registration, listener);

        Object session = getSessions().get("inst-fail");
        Assertions.assertNotNull(session);

        invokeHeartbeatTick(session);

        Assertions.assertNotNull(listener.lostRegistration.get(), "listener must be called on failure");
        Assertions.assertEquals("inst-fail", listener.lostRegistration.get().getInstanceId());
        Assertions.assertEquals("HTTP heartbeat failed", listener.lostReason.get());
        Assertions.assertFalse(getSessions().containsKey("inst-fail"), "failed session must be removed");
    }

    // -----------------------------------------------------------------------
    // heartbeatTick — discoveryClient is null: tick is a no-op
    // -----------------------------------------------------------------------

    @Test
    void heartbeatTick_withNullDiscoveryClient_isNoOp() throws Exception {
        // do NOT set a discoveryClient
        RecordingListener listener = new RecordingListener();
        ServiceLivenessRegistration registration = reg("svc", "inst-nodc");
        client.start(registration, listener);

        Object session = getSessions().get("inst-nodc");
        Assertions.assertNotNull(session);

        Assertions.assertDoesNotThrow(() -> invokeHeartbeatTick(session),
                "heartbeatTick with null discoveryClient must not throw");
        Assertions.assertNull(listener.lostRegistration.get(), "listener must NOT be called when client is null");
    }

    // -----------------------------------------------------------------------
    // heartbeatTick — running=false (already stopped): tick is a no-op
    // -----------------------------------------------------------------------

    @Test
    void heartbeatTick_afterStop_isNoOp() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(false); // would fail if called
        client.setDiscoveryClient(dc);

        RecordingListener listener = new RecordingListener();
        ServiceLivenessRegistration registration = reg("svc", "inst-stopped");
        client.start(registration, listener);

        Object session = getSessions().get("inst-stopped");
        Assertions.assertNotNull(session);

        // Deactivate sets running=false on all sessions
        client.deactivate();

        // heartbeatTick should be a no-op after stop (running=false)
        Assertions.assertDoesNotThrow(() -> invokeHeartbeatTick(session));
        Assertions.assertNull(listener.lostRegistration.get(),
                "listener must NOT be called after session is stopped");
        // Verify heartbeat was never actually invoked on the discovery client
        Assertions.assertEquals(0, dc.heartbeatCallCount.get());
    }

    // -----------------------------------------------------------------------
    // deactivate() — stops all sessions and clears the map
    // -----------------------------------------------------------------------

    @Test
    void deactivate_stopsAllSessionsAndClearsMap() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);

        client.start(reg("svc", "inst-A"), new RecordingListener());
        client.start(reg("svc", "inst-B"), new RecordingListener());

        Assertions.assertEquals(2, getSessions().size());
        client.deactivate();
        Assertions.assertTrue(getSessions().isEmpty(), "map must be empty after deactivate");
    }

    // -----------------------------------------------------------------------
    // resolveHeartbeatIntervalSeconds — no globalOptions → default 25
    // -----------------------------------------------------------------------

    @Test
    void start_withNoGlobalOptions_usesDefaultInterval25() throws Exception {
        // globalOptions not set → resolveHeartbeatIntervalSeconds returns 25
        // We verify by inspecting the intervalSeconds field of the inner session
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);

        ServiceLivenessRegistration registration = reg("svc", "inst-interval");
        client.start(registration, new RecordingListener());

        Object session = getSessions().get("inst-interval");
        Assertions.assertNotNull(session);

        Field intervalField = session.getClass().getDeclaredField("intervalSeconds");
        intervalField.setAccessible(true);
        long interval = (long) intervalField.get(session);
        Assertions.assertEquals(25L, interval, "default heartbeat interval must be 25 seconds");
    }

    // -----------------------------------------------------------------------
    // resolveHeartbeatIntervalSeconds — globalOptions returns 0 → default 25
    // -----------------------------------------------------------------------

    @Test
    void start_withZeroIntervalFromGlobalOptions_usesDefault25() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);
        client.setGlobalOptions(new StubGlobalOptions(0L));

        ServiceLivenessRegistration registration = reg("svc", "inst-zero-interval");
        client.start(registration, new RecordingListener());

        Object session = getSessions().get("inst-zero-interval");
        Field intervalField = session.getClass().getDeclaredField("intervalSeconds");
        intervalField.setAccessible(true);
        long interval = (long) intervalField.get(session);
        Assertions.assertEquals(25L, interval);
    }

    // -----------------------------------------------------------------------
    // resolveHeartbeatIntervalSeconds — globalOptions returns positive value
    // -----------------------------------------------------------------------

    @Test
    void start_withPositiveIntervalFromGlobalOptions_usesThatInterval() throws Exception {
        RecordingDiscoveryClient dc = new RecordingDiscoveryClient(true);
        client.setDiscoveryClient(dc);
        client.setGlobalOptions(new StubGlobalOptions(60L));

        ServiceLivenessRegistration registration = reg("svc", "inst-custom-interval");
        client.start(registration, new RecordingListener());

        Object session = getSessions().get("inst-custom-interval");
        Field intervalField = session.getClass().getDeclaredField("intervalSeconds");
        intervalField.setAccessible(true);
        long interval = (long) intervalField.get(session);
        Assertions.assertEquals(60L, interval);
    }

    // -----------------------------------------------------------------------
    // Stub for ServiceDiscoveryGlobalOptions
    // -----------------------------------------------------------------------

    private static final class StubGlobalOptions implements ServiceDiscoveryGlobalOptions {
        private final long heartbeatInterval;

        StubGlobalOptions(long heartbeatInterval) {
            this.heartbeatInterval = heartbeatInterval;
        }

        @Override public String getDiscoveryUrl() { return ""; }
        @Override public String getDefaultHost() { return ""; }
        @Override public long getHeartbeatIntervalSeconds() { return heartbeatInterval; }
        @Override public long getRegistrationRetryInitialDelaySeconds() { return 30L; }
        @Override public long getRegistrationRetryMaxDelaySeconds() { return 300L; }
        @Override public long getHttpTimeoutSeconds() { return 10L; }
        @Override public int getRegistrationMaxAttempts() { return 3; }
        @Override public long[] getRegistrationRetryBackoffMs() { return new long[]{2000L, 4000L, 8000L}; }
    }
}
