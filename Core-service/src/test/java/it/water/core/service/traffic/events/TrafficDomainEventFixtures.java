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
package it.water.core.service.traffic.events;

import it.water.core.api.entity.events.PostRemoveEvent;
import it.water.core.api.entity.events.PostSaveEvent;
import it.water.core.api.entity.events.PostUpdateDetailedEvent;
import it.water.core.api.entity.events.PostUpdateEvent;
import it.water.core.api.entity.events.PreRemoveEvent;
import it.water.core.api.entity.events.PreSaveEvent;
import it.water.core.api.entity.events.PreUpdateDetailedEvent;
import it.water.core.api.entity.events.PreUpdateEvent;
import it.water.core.api.model.BaseEntity;
import it.water.core.api.model.Resource;
import it.water.core.api.model.events.Event;

import java.util.Date;

/**
 * Plain, TOP-LEVEL (deliberately NOT nested static classes) test fixtures shared by
 * {@link TrafficDomainEventListenerTest}. Kept top-level so that {@code Class#getName()} yields a
 * clean, dot-suffixed simple name for each fixture: {@link TrafficDomainEventListener} derives the
 * whitelist "simple name" match from the LAST DOT in the fully qualified class name, and a nested
 * class would instead produce an {@code Outer$Inner}-shaped name that defeats that scenario.
 * <p>
 * None of these fixtures need to be {@code public}: they are consumed only from within this same
 * package by plain, direct Mockito-based unit tests (no dynamic proxy / reflection-based
 * invocation is involved, unlike the {@code TestServiceProxy} harness pattern used elsewhere).
 */
final class TestDomainEntity implements BaseEntity {

    private final long id;

    TestDomainEntity(long id) {
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
        // not exercised by these tests
    }
}

/**
 * A {@link Resource} that is deliberately NOT a {@link BaseEntity}: exercises the
 * {@code resourceId == null} branch of {@link TrafficDomainEventListener#resourceIdOf(Resource)}.
 */
class TestPlainResource implements Resource {
}

class TestPreSaveEvent implements PreSaveEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity entity) {
        // no-op fixture
    }
}

class TestPostSaveEvent implements PostSaveEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity entity) {
        // no-op fixture
    }
}

class TestPreUpdateEvent implements PreUpdateEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity entity) {
        // no-op fixture
    }
}

class TestPostUpdateEvent implements PostUpdateEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity entity) {
        // no-op fixture
    }
}

class TestPreUpdateDetailedEvent implements PreUpdateDetailedEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity beforeCrudOperation, TestDomainEntity afterCrudOperation) {
        // no-op fixture
    }
}

class TestPostUpdateDetailedEvent implements PostUpdateDetailedEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity beforeCrudOperation, TestDomainEntity afterCrudOperation) {
        // no-op fixture
    }
}

class TestPreRemoveEvent implements PreRemoveEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity entity) {
        // no-op fixture
    }
}

class TestPostRemoveEvent implements PostRemoveEvent<TestDomainEntity> {
    @Override
    public void execute(TestDomainEntity entity) {
        // no-op fixture
    }
}

/**
 * A domain {@link Event} that is none of the CRUD specializations: exercises the
 * {@code GENERIC} change-operation / {@code DOMAIN_EVENT} record-type / {@code null} change-phase
 * mapping branch.
 */
class TestGenericEvent implements Event {
}
