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

import it.water.core.api.service.integration.discovery.ServiceDiscoveryGlobalOptions;
import it.water.core.api.service.integration.discovery.ServiceLivenessClient;
import it.water.core.api.service.integration.discovery.ServiceLivenessListener;
import it.water.core.api.service.integration.discovery.ServiceLivenessRegistration;
import it.water.core.api.service.integration.discovery.ServiceLivenessSession;
import it.water.core.api.interceptors.OnDeactivate;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@FrameworkComponent(priority = 1, services = ServiceLivenessClient.class)
public class HttpHeartbeatServiceLivenessClient implements ServiceLivenessClient {
    private static final long DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 25L;

    @Inject
    @Setter
    private ServiceDiscoveryRegistryClientInternal discoveryClient;

    @Inject
    @Setter
    private ServiceDiscoveryGlobalOptions globalOptions;

    private final Map<String, HttpHeartbeatSession> sessions = new ConcurrentHashMap<>();

    @OnDeactivate
    public void deactivate() {
        sessions.values().forEach(HttpHeartbeatSession::stop);
        sessions.clear();
    }

    @Override
    public ServiceLivenessSession start(ServiceLivenessRegistration registration, ServiceLivenessListener listener) {
        HttpHeartbeatSession session = new HttpHeartbeatSession(registration, listener, resolveHeartbeatIntervalSeconds());
        HttpHeartbeatSession previous = sessions.put(registration.getInstanceId(), session);
        if (previous != null) {
            previous.stop();
        }
        session.start();
        return () -> {
            HttpHeartbeatSession current = sessions.remove(registration.getInstanceId());
            if (current != null) {
                current.stop();
            }
        };
    }

    private long resolveHeartbeatIntervalSeconds() {
        if (globalOptions == null) {
            return DEFAULT_HEARTBEAT_INTERVAL_SECONDS;
        }
        long configuredValue = globalOptions.getHeartbeatIntervalSeconds();
        return configuredValue > 0 ? configuredValue : DEFAULT_HEARTBEAT_INTERVAL_SECONDS;
    }

    private final class HttpHeartbeatSession implements ServiceLivenessSession {
        private final ServiceLivenessRegistration registration;
        private final ServiceLivenessListener listener;
        private final long intervalSeconds;
        private final ScheduledExecutorService scheduler;
        private volatile boolean running = true;

        private HttpHeartbeatSession(ServiceLivenessRegistration registration,
                                     ServiceLivenessListener listener,
                                     long intervalSeconds) {
            this.registration = registration;
            this.listener = listener;
            this.intervalSeconds = intervalSeconds;
            this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable,
                        "heartbeat-" + registration.getServiceName() + "-" + registration.getInstanceId());
                thread.setDaemon(true);
                return thread;
            });
        }

        private void start() {
            scheduler.scheduleAtFixedRate(this::heartbeatTick, intervalSeconds, intervalSeconds, TimeUnit.SECONDS);
        }

        private void heartbeatTick() {
            if (!running || discoveryClient == null) {
                return;
            }
            boolean ok = discoveryClient.heartbeat(registration.getServiceName(), registration.getInstanceId());
            if (ok) {
                return;
            }
            running = false;
            sessions.remove(registration.getInstanceId(), this);
            stop();
            if (listener != null) {
                listener.onLivenessLost(registration, "HTTP heartbeat failed");
            }
        }

        @Override
        public void stop() {
            running = false;
            scheduler.shutdownNow();
        }
    }
}
