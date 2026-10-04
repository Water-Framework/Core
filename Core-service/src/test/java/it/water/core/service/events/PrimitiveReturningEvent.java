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
package it.water.core.service.events;

import it.water.core.api.model.events.Event;

/**
 * Test-only fixture {@link Event} declaring one method per JDK primitive return type, plus one
 * {@code void} and one {@code Object} ({@link String}) returning method. Used exclusively by
 * {@link InMemoryApplicationEventProducerTest} to drive every branch of the private
 * {@code InMemoryApplicationEventProducer.EventInvocationHandler#defaultValueFor(Class)} helper,
 * which supplies a safe default return value for the synthetic event proxy's methods (the event
 * itself carries no behaviour - see {@link InMemoryApplicationEventProducer}'s class Javadoc: the
 * {@code ApplicationEventListener} callbacks are the real delivery channel).
 * <p>
 * Deliberately PUBLIC and TOP-LEVEL (not a nested/static inner class): {@link java.lang.reflect.Proxy}
 * needs to implement exactly this interface, and keeping it top-level avoids the
 * {@code Outer$Inner} class-name gotcha already documented on the other fixtures in this test tree
 * (irrelevant to the whitelist-matching scenarios here, but kept as the consistent convention).
 */
public interface PrimitiveReturningEvent extends Event {
    void doVoid();

    boolean doBoolean();

    char doChar();

    long doLong();

    float doFloat();

    double doDouble();

    int doInt();

    byte doByte();

    short doShort();

    String doObject();
}
