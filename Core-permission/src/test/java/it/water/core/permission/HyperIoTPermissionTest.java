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

import it.water.core.api.action.Action;
import it.water.core.api.model.Resource;
import it.water.core.api.permission.PermissionManager;
import it.water.core.model.exceptions.WaterRuntimeException;
import it.water.core.permission.action.*;
import it.water.core.permission.action.model.TestActionsManager;
import it.water.core.permission.action.model.TestResource;
import it.water.core.permission.exceptions.UnauthorizedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        DefaultResourceAction resourceAction = ActionFactory.createResourceAction(TestResource.class, CrudAction.FIND);
        assertNotNull(resourceAction);
        Assertions.assertEquals(4, resourceAction.getAction().getActionId());
        Assertions.assertEquals(CrudAction.FIND, resourceAction.getAction());
        Constructor<ActionFactory> constructor = ActionFactory.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, () -> {
            constructor.newInstance();
        });
    }

    @Test
    void crudActionShouldWorkAsExcepted() {
        CrudAction action = CrudAction.FIND;
        Assertions.assertEquals(CrudAction.FIND.getActionId(), action.getActionId());
        Assertions.assertEquals(CrudAction.FIND.getActionName(), action.getActionName());
        Assertions.assertEquals(CrudAction.FIND.getActionType(), action.getActionType());
    }

    @Test
    void apiActionShouldWorkAsExcepted() {
        WebAPIAction action = WebAPIAction.GET;
        Assertions.assertEquals(WebAPIAction.GET.getActionId(), action.getActionId());
        Assertions.assertEquals(WebAPIAction.GET.getActionName(), action.getActionName());
        Assertions.assertEquals(WebAPIAction.GET.getActionType(), action.getActionType());
    }

    @Test
    void shareActionShouldWorkAsExcepted() {
        ShareAction action = ShareAction.SHARE;
        Assertions.assertEquals(ShareAction.SHARE.getActionId(), action.getActionId());
        Assertions.assertEquals(ShareAction.SHARE.getActionName(), action.getActionName());
        Assertions.assertEquals(ShareAction.SHARE.getActionType(), action.getActionType());
    }

    @Test
    void actionListShouldWorkAsExpected() {
        DefaultActionList actionList = ActionFactory.createBaseCrudActionList(TestResource.class);
        //adding one action that should maintain the order
        actionList.addAction(WebAPIAction.GET);
        Assertions.assertNotNull(actionList.toString());
        List<DefaultResourceAction> resourceActions = actionList.getList();
        Map<String, List<Action>> actionsTypesMap = new HashMap<>();
        //same order as they are put inside actions factory
        actionsTypesMap.put(CrudAction.class.getName(), new ArrayList<>());
        actionsTypesMap.put(WebAPIAction.class.getName(), new ArrayList<>());
        actionsTypesMap.get(CrudAction.SAVE.getActionType()).add(CrudAction.SAVE);
        actionsTypesMap.get(CrudAction.UPDATE.getActionType()).add(CrudAction.UPDATE);
        actionsTypesMap.get(CrudAction.REMOVE.getActionType()).add(CrudAction.REMOVE);
        actionsTypesMap.get(CrudAction.FIND.getActionType()).add(CrudAction.FIND);
        actionsTypesMap.get(WebAPIAction.GET.getActionType()).add(WebAPIAction.GET);

        //should be ordered

        for (int i = 0; i < resourceActions.size(); i++) {
            DefaultResourceAction resourceAction = resourceActions.get(i);
            long actionId = resourceAction.getAction().getActionId();
            //assert pow of two
            Assertions.assertEquals(TestResource.class, resourceAction.getResourceClass());
            assertEquals(0, (actionId & (actionId - 1)));
            assertTrue(actionsTypesMap.get(resourceAction.getAction().getActionType()).contains(resourceAction.getAction()));
            actionsTypesMap.get(resourceAction.getAction().getActionType()).remove(resourceAction.getAction());
            if (actionsTypesMap.get(resourceAction.getAction().getActionType()).isEmpty())
                actionsTypesMap.remove(resourceAction.getAction().getActionType());
        }
        assertTrue(actionsTypesMap.isEmpty());
    }

    @Test
    void testStandardActionsManager() {
        TestActionsManager testActionsManager = new TestActionsManager();
        testActionsManager.registerActions();
        Assertions.assertFalse(testActionsManager.getActions().isEmpty());
    }
}
