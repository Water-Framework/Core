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
package it.water.core.bundle;

import it.water.core.api.service.Service;
import it.water.core.api.service.cluster.ClusterCoordinatorClient;
import it.water.core.api.service.cluster.ClusterNodeInfo;
import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.api.service.cluster.ClusterObserver;
import it.water.core.interceptors.annotations.FrameworkComponent;

import java.util.Collection;
import java.util.List;

/**
 * @Author Aristide Cittadino
 * Testing component registration without services attribute inside @FrameworkComponent.
 * This cause the component registry to register this class with the implemented interfaces, in
 * this case Service.
 */
@FrameworkComponent
public class FakeClusterCoordinatorClient implements ClusterCoordinatorClient {

    @Override
    public boolean registerToCluster() {
        return false;
    }

    @Override
    public boolean unregisterToCluster() {
        return false;
    }

    @Override
    public boolean checkClusterLeadershipFor(String taskId) {
        return false;
    }

    @Override
    public boolean peerStillExists(ClusterNodeOptions clusterNodeOptions) {
        return false;
    }

    @Override
    public Collection<ClusterNodeInfo> getPeerNodes(ClusterNodeOptions clusterNodeOptions) {
        return List.of();
    }

    @Override
    public void subscribeToClusterEvents(ClusterObserver clusterObserver) {
        //do nothing
    }

    @Override
    public void unsubscribeToClusterEvents(ClusterObserver clusterObserver) {
        //do nothing
    }
}
