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

import it.water.core.api.service.cluster.ClusterNodeOptions;
import it.water.core.interceptors.annotations.FrameworkComponent;


/**
 * @Author Aristide Cittadino
 * Testing component registration without services attribute inside @FrameworkComponent.
 * This cause the component registry to register this class with the implemented interfaces, in
 * this case Service.
 */
@FrameworkComponent
public class FakeClusterNodeOptions implements ClusterNodeOptions {
    @Override
    public boolean clusterModeEnabled() {
        return false;
    }

    @Override
    public String getNodeId() {
        return "";
    }

    @Override
    public String getLayer() {
        return "";
    }

    @Override
    public String getIp() {
        return "";
    }

    @Override
    public String getHost() {
        return "";
    }

    @Override
    public boolean useIpInClusterRegistration() {
        return false;
    }
}
