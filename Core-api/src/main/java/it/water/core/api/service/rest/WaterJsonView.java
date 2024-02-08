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

package it.water.core.api.service.rest;

/**
 * @Author Aristide Cittadino
 * JSON Views that can be associated to entity and rest services.
 */
public interface WaterJsonView {

    /**
     * Public view: All public fields that all users can access.
     */
    interface Public extends Extended {
    }

    /**
     * Compact View: reduces the amount of fields shown, just necessary one.
     * This view includes fields inside Public view.
     */
    interface Compact extends Public {
    }

    /**
     * Extended View: increase the amount of fields show with deeper details.
     * This view includes fields inside Public view.
     */
    interface Extended {
    }

    /**
     * Internal View: Internal fields for interal system use
     */
    interface Internal {
    }

    /**
     * Secured View: Secured fields that must not be exposed outside
     */
    interface Secured {
    }

    /**
     * Privacy View: Privacy Fields
     */
    interface Privacy {
    }

}
