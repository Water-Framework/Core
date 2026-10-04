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
package it.water.core.service.traffic;

import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.traffic.annotations.NoTraffic;
import it.water.core.api.traffic.annotations.ReportTraffic;
import it.water.core.interceptors.annotations.Inject;
import lombok.Setter;

/**
 * Test-only implementation of {@link TestS2SApi}, covering the three shapes the opt-out capture has
 * to get right: a method with {@code @ReportTraffic} (which now only OVERRIDES the operation label),
 * a method with no annotation at all (captured all the same - that is the whole point of opt-out),
 * and a method with {@code @NoTraffic} (excluded at the source).
 * <p>
 * Annotations are declared on this concrete class rather than only on the interface because the
 * capture re-reads them from the implementation as well; either placement works, and both are
 * exercised here.
 */
public class TestS2SServiceImpl implements TestS2SApi {

    /**
     * Resolved lazily through the registry rather than injected as the SystemApi itself: the nested
     * call has to go through the PROXY to be intercepted, and the registry is what hands out proxies.
     */
    @Inject
    @Setter
    private ComponentRegistry componentRegistry;

    @Override
    @ReportTraffic(operation = "doWork")
    public String doWork(String input) {
        return "handled:" + input;
    }

    @Override
    @ReportTraffic
    public String doWorkDefaultOperation(String input) {
        return "handled-default:" + input;
    }

    @Override
    @ReportTraffic(operation = "doWorkThrows")
    public String doWorkThrows(String input) {
        throw new IllegalStateException("business failure: " + input);
    }

    @Override
    public String doPlainWork(String input) {
        return "handled-plain:" + input;
    }

    @Override
    @NoTraffic
    public String doSilentWork(String input) {
        return "handled-silent:" + input;
    }

    @Override
    public String doNestedWork(String input) {
        TestS2SSystemApi systemApi = componentRegistry.findComponent(TestS2SSystemApi.class, null);
        return "handled-nested:" + systemApi.doSystemWork(input);
    }
}
