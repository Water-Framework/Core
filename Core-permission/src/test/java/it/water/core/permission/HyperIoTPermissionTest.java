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
package it.water.core.permission;

import it.water.core.api.action.ActionList;
import it.water.core.api.model.Resource;
import it.water.core.api.permission.PermissionManager;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.action.*;
import it.water.core.permission.action.model.TestResource;
import it.water.core.permission.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WaterPermissionTest {
    @Test
    void testUnauthorizedExceptionGetMessage() {
        WaterRuntimeException ex = new UnauthorizedException("message");
        Assertions.assertNotNull(ex);
    }

    @Test
    void testEntityIsNotProtected() {
        Object entity = new Object();
        Assertions.assertFalse(PermissionManager.isProtectedEntity(entity));
    }

    @Test
    void testEntityIsProtected() {
        ProtectedEntity entity = new ProtectedEntity();
        Assertions.assertTrue(PermissionManager.isProtectedEntity(entity));
    }

    @Test
    void testResourceIsProtected() {
        ProtectedResource resource = new ProtectedResource();
        Assertions.assertTrue(PermissionManager.isProtectedEntity(resource));
    }

    @Test
    void testEntityIsNotProtectedWithClassName() {
        String className = "java.lang.String";
        Assertions.assertFalse(PermissionManager.isProtectedEntity(className));
    }

    @Test
    void testEntityIsProtectedWithClassName() {
        String className = "it.water.core.permission.ProtectedEntity";
        Assertions.assertTrue(PermissionManager.isProtectedEntity(className));
    }

    @Test
    void testResourceIsProtectedWithClassName() {
        String className = "it.water.core.permission.ProtectedResource";
        Assertions.assertTrue(PermissionManager.isProtectedEntity(className));
    }

    @Test
    void testIsNotProtectedWithNoExistingClassName() {
        String className = "it.water.core.permission.NoExistsClass";
        Assertions.assertTrue(PermissionManager.isProtectedEntity(className));
    }

    @Test
    void actionFactoryShouldCreateResourceAction() throws InvocationTargetException, IllegalStateException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        Assertions.assertNotNull(ActionFactory.createEmptyActionList(TestResource.class));
        Resource resource = new TestResource();
        assertEquals(resource.getResourceName(), resource.getClass().getName());
        DefaultResourceAction resourceAction = ActionFactory.createResourceAction(TestResource.class, ActionFactory.createGenericAction(TestResource.class, CrudActions.FIND, 1));
        assertNotNull(resourceAction);
        Assertions.assertEquals(1, resourceAction.getAction().getActionId());
        Assertions.assertEquals(CrudActions.FIND, resourceAction.getAction().getActionName());
        Constructor<ActionFactory> constructor = ActionFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
    }

    @Test
    void actionListShouldWorkAsExpected() {
        DefaultActionList actionList = ActionFactory.createBaseCrudActionList(TestResource.class);
        //adding one action that should maintain the order
        actionList.addAction(ActionFactory.createGenericAction(TestResource.class, ShareAction.SHARE, 16));
        Assertions.assertNotNull(actionList.toString());
        List<DefaultResourceAction> resourceActions = actionList.getList();
        List<String> actionsListNames = new ArrayList<>();
        //same order as they are put inside actions factory
        actionsListNames.add(CrudActions.SAVE);
        actionsListNames.add(CrudActions.UPDATE);
        actionsListNames.add(CrudActions.REMOVE);
        actionsListNames.add(CrudActions.FIND);
        actionsListNames.add(ShareAction.SHARE);
        //should be ordered
        for (int i = 0; i < resourceActions.size(); i++) {
            DefaultResourceAction resourceAction = resourceActions.get(i);
            long actionId = resourceAction.getAction().getActionId();
            //assert pow of two
            Assertions.assertEquals(TestResource.class, resourceAction.getResourceClass());
            assertEquals(0, (actionId & (actionId - 1)));
            assertTrue(actionsListNames.contains(resourceAction.getAction().getActionName()));
            actionsListNames.remove(resourceAction.getAction().getActionName());
        }
        assertTrue(actionsListNames.isEmpty());
    }

    @Test
    void actionManagerShouldRegisterResourceActions() {
        DefaultActionsManager defaultActionsManager = new DefaultActionsManager();
        defaultActionsManager.setPermissionManager(new FakePermissionManager());
        defaultActionsManager.setRoleManager(new FakeRoleManager());
        ProtectedEntity protectedEntity = new ProtectedEntity();
        protectedEntity.setEntityVersion(1);
        defaultActionsManager.registerActions(protectedEntity);
        ActionList<?> actions = defaultActionsManager.getActions().get(protectedEntity.getResourceName());
        Assertions.assertTrue(actions.containsActionName(CrudActions.SAVE));
        Assertions.assertTrue(actions.containsActionName(CrudActions.UPDATE));
        Assertions.assertTrue(actions.containsActionName(CrudActions.REMOVE));
        Assertions.assertTrue(actions.containsActionName(CrudActions.FIND));
        Assertions.assertFalse(actions.containsActionName(ShareAction.SHARE));
    }

}
