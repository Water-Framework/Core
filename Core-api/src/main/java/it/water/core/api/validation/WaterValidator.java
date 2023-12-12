package it.water.core.api.validation;

import it.water.core.api.model.Resource;

public interface WaterValidator {
    void validate(Resource entity);
}
