package it.water.core.api.service;

import it.water.core.api.model.BaseEntity;

/**
 * @Author Aristide Cittadino
 * This interface is used to register component for entity extension.
 * It has:
 * - related type:  the entity the extension refers to
 * - type : the extension concrete type
 * The developer must define a service which implements this interface registered as a component in order
 * to have his extension available
 */
public interface EntityExtensionService extends Service {
    public static final String RELATED_ENTITY_PROPERTY = "waterEntityExtensionType";

    /**
     * @return related entity type
     */
    Class<? extends BaseEntity> relatedType();

    /**
     * Extension concrete type
     *
     * @return
     */
    Class<? extends BaseEntity> type();
}
