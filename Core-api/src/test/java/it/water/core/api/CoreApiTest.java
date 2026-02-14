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

package it.water.core.api;

import it.water.core.api.model.Resource;
import it.water.core.api.model.Role;
import it.water.core.api.permission.PermissionManagerComponentProperties;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.operands.FieldNameOperand;
import it.water.core.api.repository.query.operands.FieldValueListOperand;
import it.water.core.api.repository.query.operands.FieldValueOperand;
import it.water.core.api.repository.query.operands.ParenthesisNode;
import it.water.core.api.repository.query.operations.AbstractOperation;
import it.water.core.api.repository.query.operations.In;
import it.water.core.api.repository.query.operations.NotEqualTo;
import it.water.core.api.service.cluster.ClusterEvent;
import it.water.core.api.service.integration.discovery.ServiceDiscoveryServerProperties;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class CoreApiTest {

    @Test
    void checkOperands() {
        FieldNameOperand fieldNameOperand = new FieldNameOperand("fieldName");
        String strDefinition = fieldNameOperand.equalTo("'prova'").getDefinition();
        Assertions.assertEquals("fieldName = 'prova'", strDefinition);
        strDefinition = fieldNameOperand.notEqualTo("'prova'").getDefinition();
        Assertions.assertEquals("fieldName <> 'prova'", strDefinition);
        Assertions.assertEquals("NotEqualTo (!=)",((NotEqualTo)fieldNameOperand.notEqualTo("'prova'")).getName());
        strDefinition = fieldNameOperand.like("'prova'").getDefinition();
        Assertions.assertEquals("fieldName LIKE 'prova'", strDefinition);
        AbstractOperation abstractOperation = (AbstractOperation) fieldNameOperand.greaterOrEqualThan(10);
        Assertions.assertEquals("GreaterOrEqualThan (>=)",abstractOperation.getName());
        Assertions.assertEquals(">=",abstractOperation.getOperator());
        Assertions.assertFalse(abstractOperation.needsExpr());
        Assertions.assertNotNull(abstractOperation.getOperand(0));
        Assertions.assertNotNull(abstractOperation.operands());
        Assertions.assertThrows(IllegalArgumentException.class, () -> abstractOperation.defineOperands(fieldNameOperand,fieldNameOperand,fieldNameOperand));
        strDefinition = fieldNameOperand.greaterOrEqualThan(10).getDefinition();
        Assertions.assertEquals("fieldName >= 10", strDefinition);
        strDefinition = fieldNameOperand.greaterThan(10).getDefinition();
        Assertions.assertEquals("fieldName > 10", strDefinition);
        strDefinition = fieldNameOperand.lowerOrEqualThan(10).getDefinition();
        Assertions.assertEquals("fieldName <= 10", strDefinition);
        strDefinition = fieldNameOperand.lowerThan(10).getDefinition();
        Assertions.assertEquals("fieldName < 10", strDefinition);
        strDefinition = fieldNameOperand.notEqualTo("'prova'").getDefinition();
        Assertions.assertEquals("fieldName <> 'prova'", strDefinition);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fieldNameOperand.and(null));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> fieldNameOperand.or(null));
        Assertions.assertThrows(UnsupportedOperationException.class, fieldNameOperand::not);
        FieldValueOperand fieldValueOperand = new FieldValueOperand("value");
        Assertions.assertEquals("value", fieldValueOperand.getValue());
        ParenthesisNode parenthesisNode = new ParenthesisNode();
        Assertions.assertNotNull(parenthesisNode);
        parenthesisNode.defineOperands(fieldNameOperand);
        Assertions.assertEquals("( fieldName )",parenthesisNode.getDefinition());
    }

    @Test
    void checkOperations() {
        FieldNameOperand fieldNameOperand = new FieldNameOperand("fieldName");
        String definition = fieldNameOperand.equalTo("'prova'").and(fieldNameOperand.equalTo("'prova1'")).getDefinition();
        Assertions.assertEquals("fieldName = 'prova' AND fieldName = 'prova1'", definition);
        definition = fieldNameOperand.equalTo("'prova'").or(fieldNameOperand.equalTo("'prova1'")).getDefinition();
        Assertions.assertEquals("fieldName = 'prova' OR fieldName = 'prova1'", definition);
        definition = fieldNameOperand.equalTo("'prova'").or(fieldNameOperand.equalTo("'prova1'")).getDefinition();
        Assertions.assertEquals("fieldName = 'prova' OR fieldName = 'prova1'", definition);
        definition = fieldNameOperand.equalTo("'prova'").not().getDefinition();
        Assertions.assertEquals("NOT (fieldName = 'prova')", definition);
        In inOperation = new In();
        inOperation.defineOperands(fieldNameOperand, new FieldValueListOperand(List.of("'prova1'", "'prova2'", "'prova3'", "'prova4'")));
        definition = inOperation.getDefinition();
        Assertions.assertEquals("fieldName IN ('prova1','prova2','prova3','prova4')", definition);
        Query greaterThan = fieldNameOperand.greaterThan(10);
        Assertions.assertThrows(IllegalArgumentException.class, () -> greaterThan.and(null));
        Assertions.assertThrows(IllegalArgumentException.class, () -> greaterThan.or(null));
    }

    @Test
    void coverageFix(){
        Assertions.assertNotNull(ServiceDiscoveryServerProperties.SERVICE_DISCOVERY_IN_MEMORY_SERVER_IMPLEMENTATION);
        Assertions.assertNotNull(ServiceDiscoveryServerProperties.SERVICE_DISCOVERY_IN_MEMORY_SERVER_IMPLEMENTATION);
        Assertions.assertNotNull(PermissionManagerComponentProperties.PERMISSION_MANAGER_DEFAILT_IMPLEMENTATION);
        Assertions.assertNotNull(PermissionManagerComponentProperties.PERMISSION_MANAGER_DEFAILT_IMPLEMENTATION);
        Assertions.assertNotNull(ClusterEvent.PEER_CONNECTED);
        Assertions.assertNotNull(ClusterEvent.PEER_CUSTOM_EVENT);
        Assertions.assertNotNull(ClusterEvent.PEER_DATA_EVENT);
        Assertions.assertNotNull(ClusterEvent.PEER_DISCONNECTED);
        Assertions.assertNotNull(ClusterEvent.PEER_INFO_CHANGED);
        Assertions.assertNotNull(ClusterEvent.PEER_ERROR);
        Assertions.assertEquals(0L,new Role() {
            @Override
            public String getName() {
                return "role";
            }
        }.getId());
        Assertions.assertEquals("it.water.core.api.CoreApiTest$2",new Resource() {
            @Override
            public String getResourceName() {
                return Resource.super.getResourceName();
            }
        }.getResourceName());
    }
}
