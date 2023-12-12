
/*
 Copyright 2019-2023 ACSoftware

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 */

package it.water.core.service;

import it.water.core.api.model.Resource;
import it.water.core.api.validation.WaterValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Author Aristide Cittadino
 */
public abstract class GenericSystemServiceImpl extends BaseSystemServiceImpl {
    private static Logger log = LoggerFactory.getLogger(GenericSystemServiceImpl.class);

    @Override
    protected void validate(Resource entity) {
        log.debug("Validating entity {}", entity);
        getValidatorInstance().validate(entity);
    }

    protected abstract WaterValidator getValidatorInstance();

}
