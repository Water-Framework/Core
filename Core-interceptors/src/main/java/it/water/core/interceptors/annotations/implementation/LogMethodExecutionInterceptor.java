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

package it.water.core.interceptors.annotations.implementation;

import it.water.core.api.interceptors.BeforeMethodInterceptor;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.interceptors.WaterAbstractInterceptor;
import it.water.core.interceptors.annotations.FrameworkComponent;
import it.water.core.interceptors.annotations.Inject;
import it.water.core.interceptors.annotations.LogMethodExecution;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author Aristide Cittadino
 * <p>
 * Logs every method or class annotated with LogMethodExecution.
 */
@FrameworkComponent(services = {BeforeMethodInterceptor.class})
public class LogMethodExecutionInterceptor extends WaterAbstractInterceptor<Service> implements BeforeMethodInterceptor<LogMethodExecution> {
    @Inject
    @Setter
    @Getter
    private ComponentRegistry componentsRegistry;

    private Map<Class<?>, Logger> loggersMap;

    public LogMethodExecutionInterceptor() {
        loggersMap = new HashMap<>();
    }

    @Override
    public <S extends Service> void interceptMethod(S destination, Method m, Object[] args, LogMethodExecution annotation) {
        Class<?> destinationRealClass = computeServiceClass(destination);
        loggersMap.computeIfAbsent(destinationRealClass, key -> LoggerFactory.getLogger(destinationRealClass));
        StringBuilder sb = new StringBuilder();
        sb.append("Invoking " + m.getName());
        if (args.length > 0)
            sb.append(", args: ");
        Arrays.stream(args).forEach(arg -> sb.append(arg.toString()));
        if (annotation.logDebug() && loggersMap.get(destinationRealClass).isDebugEnabled()) {
            loggersMap.get(destinationRealClass).debug(sb.toString());
        } else {
            loggersMap.get(destinationRealClass).info(sb.toString());
        }
    }

    @Override
    public Class getAnnotation() {
        return LogMethodExecution.class;
    }

    private <S> Class<?> computeServiceClass(S service) {
        if (service instanceof Service && Proxy.isProxyClass(service.getClass())) {
            InvocationHandler invocationHandler = Proxy.getInvocationHandler(service);
            if (invocationHandler instanceof WaterAbstractInterceptor) {
                WaterAbstractInterceptor<?> wai = (WaterAbstractInterceptor<?>) invocationHandler;
                return wai.getOriginalConcreteClass();
            }
            return invocationHandler.getClass();
        }
        return service.getClass();
    }
}
