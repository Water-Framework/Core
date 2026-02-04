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

package it.water.core.api.service;

import lombok.Getter;
import lombok.Setter;

/**
 * @Author Christian Claudio Rosati.
 * Abstract base class for EntityExtensionService implementations.
 * Provides the waterEntityExtensionType field required for Spring compatibility.
 *
 *
 * <p>Usage example:</p>
 * <pre>
 * {@code
 * @FrameworkComponent(properties = EntityExtensionService.RELATED_ENTITY_PROPERTY + "=com.example.Document")
 * public class DocumentExtensionService extends AbstractEntityExtensionService {
 *
 *     @Override
 *     public Class<? extends BaseEntity> relatedType() {
 *         return Document.class;
 *     }
 *
 *     @Override
 *     public Class<? extends BaseEntity> type() {
 *         return DocumentExtension.class;
 *     }
 * }
 * }
 * </pre>
 */
public abstract class AbstractEntityExtensionService implements EntityExtensionService {

    /**
     * Property used by Spring for component filtering.
     * This field is required because Spring's JavaBeans introspection
     * expects a writable property when it sees the component property
     * "waterEntityExtensionType" defined in @FrameworkComponent.
     */
    @Getter
    @Setter
    private String waterEntityExtensionType;
}
