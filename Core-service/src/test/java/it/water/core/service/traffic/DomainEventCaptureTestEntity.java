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

import it.water.core.api.model.BaseEntity;

import java.util.Date;

/**
 * Minimal {@link BaseEntity} fixture used by {@link DomainEventCaptureHarnessTest}.
 * <p>
 * Deliberately PUBLIC and TOP-LEVEL (not a nested/static inner class): the same rationale already
 * documented on the test fixtures in {@code it.water.core.service.traffic.events} applies here -
 * {@code Class#getName()} of a nested class is {@code Outer$Inner}-shaped, which would defeat any
 * whitelist "simple name" matching, and {@code TestServiceProxy} (used elsewhere in this harness
 * family for proxied service resolution) invokes methods reflectively from a different package,
 * which requires public visibility.
 */
public class DomainEventCaptureTestEntity implements BaseEntity {

    private final long id;

    public DomainEventCaptureTestEntity(long id) {
        this.id = id;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getEntityCreateDate() {
        return null;
    }

    @Override
    public Date getEntityModifyDate() {
        return null;
    }

    @Override
    public Integer getEntityVersion() {
        return null;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        // not used by these tests
    }
}
