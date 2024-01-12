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

package it.water.core.interceptors;

import it.water.core.api.interceptors.*;
import it.water.core.api.registry.ComponentRegistry;
import it.water.core.api.service.Service;
import it.water.core.registry.model.exception.NoComponentRegistryFoundException;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.lang.reflect.*;
import java.util.*;


/**
 * @Author Aristide Cittadino
 */
@AllArgsConstructor
@NoArgsConstructor
public abstract class WaterAbstractInterceptor<S extends Service> implements it.water.core.api.interceptors.Proxy {
    private static Logger log = LoggerFactory.getLogger(WaterAbstractInterceptor.class);

    //original service
    @Getter(AccessLevel.PROTECTED)
    @Setter(AccessLevel.PROTECTED)
    private S service;


    /**
     * Loads all BeforeMethodInterceptor Components registered as OSGi services and execute them before method invocation
     *
     * @param method
     * @param args
     * @throws NoSuchMethodException
     */
    protected void executeInterceptorBeforeMethod(S service, Method method, Object[] args) throws NoSuchMethodException {
        this.executeInterceptor(service, method, args, null, BeforeMethodInterceptor.class);
    }

    /**
     * Loads all AfterMethodInterceptor Components registered as OSGi services and execute them after method invocation
     *
     * @param method
     * @param args
     * @param result
     * @throws NoSuchMethodException
     */
    protected void executeInterceptorAfterMethod(S service, Method method, Object[] args, Object result) throws NoSuchMethodException {
        this.executeInterceptor(service, method, args, result, AfterMethodInterceptor.class);
    }

    /**
     * Analyzes the method invocation searching for WaterInterceptorExecutor Annotation.
     *
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @throws NoSuchMethodException
     */
    protected void executeInterceptor(S service, Method method, Object[] args, Object result, Class<? extends MethodInterceptor> interceptorClass) throws NoSuchMethodException {
        interceptAnnotationsOnFields(service, method, args, result, interceptorClass);
        interceptAnnotationsOnMethod(service, method, args, result, interceptorClass);
    }


    /**
     * Returns the original component generic interfaces
     *
     * @return
     */
    public Type[] getOriginalGenericInterfaces() {
        return this.getService().getClass().getGenericInterfaces();
    }

    @Override
    public Class<?> getOriginalConcreteClass() {
        return this.getService().getClass();
    }

    protected abstract ComponentRegistry getComponentsRegistry();

    /**
     * Method which scans fields searching for annotations which are related to WaterInterceptorExecutor.
     * If it is found, it means that the current field must be intercepted by the defined interceptor inside the annotation or a registed interceptor component.
     *
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     */
    private void interceptAnnotationsOnFields(S service, Method method, Object[] args, Object result, Class<? extends MethodInterceptor> interceptorClass) {
        Map<Annotation, List<Field>> annotationsMap = new HashMap<>();
        Arrays.stream(getAllDeclaredFields(service)).forEach(field -> Arrays.stream(field.getDeclaredAnnotations())
                //ex. WaterInject annotation that are not injected at startup
                .filter(annotation -> annotation.annotationType().isAnnotationPresent(InterceptorExecutor.class)).forEach(annotation -> {
                    if (!annotationsMap.containsKey(annotation)) annotationsMap.put(annotation, new ArrayList<>());
                    annotationsMap.get(annotation).add(field);
                }));
        annotationsMap.keySet().iterator().forEachRemaining(annotation -> {
            boolean intercepted = interceptBasedOnAnnotationInterceptorExecutor(annotation, annotationsMap.get(annotation), service, method, args, result, interceptorClass);
            if (!intercepted)
                interceptBasedOnRegisterdInterceptorExecutor(annotation, annotationsMap.get(annotation), service, method, args, result, interceptorClass);
        });
    }

    /**
     * Method which scans method searching for annotations extending WaterInterceptorExecutor.
     * If found this means that the current field must be intercepted by the defined interceptor inside the annotation ir a registered interceptor component..
     *
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @throws NoSuchMethodException
     */
    private void interceptAnnotationsOnMethod(S service, Method method, Object[] args, Object result, Class<? extends MethodInterceptor> interceptorClass) throws NoSuchMethodException {
        try {
            Annotation[] annotations = service.getClass().getMethod(method.getName(), method.getParameterTypes()).getDeclaredAnnotations();
            for (int i = 0; i < annotations.length; i++) {
                Annotation annotation = annotations[i];
                //trying to intercept first looking at annotation definition, if it contains WaterInterceptorExecutor annotation definition which specifies the implementation
                boolean intercepted = interceptBasedOnAnnotationInterceptorExecutor(annotation, null, service, method, args, result, interceptorClass);
                //if no annotation WaterInterceptorExecutor is found then we search inside the registry for an implementation for that interceptor
                if (!intercepted)
                    interceptBasedOnRegisterdInterceptorExecutor(annotation, null, service, method, args, result, interceptorClass);
            }
        } catch (NoSuchMethodException e){
            log.debug(e.getMessage(),e);
        }
    }

    /**
     * Method which searches for WaterInterceptorExecutor annotation definition and execute the interceptor configured inside the annotation
     *
     * @param annotation
     * @param annotatedFields
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @return
     */
    private boolean interceptBasedOnAnnotationInterceptorExecutor(Annotation annotation, List<Field> annotatedFields, S service, Method method, Object[] args, Object result, Class<? extends MethodInterceptor> interceptorClass) {
        if (annotation.annotationType().isAnnotationPresent(InterceptorExecutor.class)) {
            InterceptorExecutor interceptorAnnotation = annotation.annotationType().getDeclaredAnnotation(InterceptorExecutor.class);
            Class<? extends MethodInterceptor> executor = interceptorAnnotation.interceptor();
            if (this.getComponentsRegistry() != null) {
                MethodInterceptor interceptor = this.getComponentsRegistry().findComponent(executor, null);
                if (interceptorClass.isAssignableFrom(interceptor.getClass())) {
                    doInterception(annotation, annotatedFields, service, method, args, result, interceptor);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Method which searches for compoents exposed as a service implementing WaterInterceptorExecutor
     *
     * @param annotation
     * @param annotatedFields
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptorClass
     * @return
     */
    private boolean interceptBasedOnRegisterdInterceptorExecutor(Annotation annotation, List<Field> annotatedFields, S service, Method method, Object[] args, Object result, Class<? extends MethodInterceptor> interceptorClass) {
        if (this.getComponentsRegistry() != null) {
            //find the executor implementation based on registerd components which expose for example BeforeMethodInterceptor or AfterMethodInterceptor
            try {
                List<? extends MethodInterceptor> interceptors = this.getComponentsRegistry().findComponents(interceptorClass, null);
                //Filter amongs all interceptors which use the current annotation as Genric Type
                Optional<? extends MethodInterceptor> executor = interceptors.stream().filter(object -> {
                    Type[] interfaces = null;
                    List<Class<?>> declaredInterfaces = new ArrayList<>(Arrays.asList(object.getClass().getInterfaces()));
                    //in case of receiving an water proxy
                    if (declaredInterfaces.contains(it.water.core.api.interceptors.Proxy.class))
                        interfaces = ((it.water.core.api.interceptors.Proxy) (Proxy.getInvocationHandler(object))).getOriginalGenericInterfaces();
                    else interfaces = object.getClass().getGenericInterfaces();
                    return Arrays.asList(interfaces).stream().anyMatch(currentInterface -> currentInterface instanceof ParameterizedType && ((ParameterizedType) currentInterface).getActualTypeArguments().length == 1 && ((ParameterizedType) currentInterface).getActualTypeArguments()[0].equals(annotation.annotationType()));
                }).findFirst();
                //if an interceptor is matched then run the interception
                if (executor.isPresent()) {
                    if (interceptorClass.isAssignableFrom(executor.get().getClass())) {
                        doInterception(annotation, annotatedFields, service, method, args, result, executor.get());
                    }
                    return true;
                }
            } catch (NoComponentRegistryFoundException e) {
                log.debug("No component found for: {}", interceptorClass);
            }
        }
        return false;
    }

    /**
     * Method which executes interception logic on field or methods based on the current interceptor type
     *
     * @param annotation
     * @param annotatedFields
     * @param service
     * @param method
     * @param args
     * @param result
     * @param interceptor
     */
    private void doInterception(Annotation annotation, List<Field> annotatedFields, S service, Method method, Object[] args, Object result, MethodInterceptor interceptor) {
        //avoiding calling after method with before methods
        if (interceptor != null) {
            //first most specific types since BeforeMethodFieldInterceptor is also BeforeMethodInterceptor
            if (BeforeMethodFieldInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                BeforeMethodFieldInterceptor<Annotation> beforeInterceptor = (BeforeMethodFieldInterceptor) interceptor;
                beforeInterceptor.interceptMethod(service, method, annotatedFields, args, annotation);
            } else if (AfterMethodFieldInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                AfterMethodFieldInterceptor<Annotation> afterInterceptor = (AfterMethodFieldInterceptor) interceptor;
                afterInterceptor.interceptMethod(service, method, annotatedFields, args, annotation);
            }
            //Then we compare generic types
            else if (BeforeMethodInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                BeforeMethodInterceptor<Annotation> beforeInterceptor = (BeforeMethodInterceptor) interceptor;
                beforeInterceptor.interceptMethod(service, method, args, annotation);
            } else if (AfterMethodInterceptor.class.isAssignableFrom(interceptor.getClass())) {
                AfterMethodInterceptor<Annotation> afterInterceptor = (AfterMethodInterceptor) interceptor;
                afterInterceptor.interceptMethod(service, method, args, result, annotation);
            }
        }
    }

    /**
     * Returs all declared fields inside a class
     *
     * @param service
     * @return
     */
    private Field[] getAllDeclaredFields(S service) {
        List<Field> fieldsList = new ArrayList<>();
        Class<?> currentClass = service.getClass();
        while (currentClass != null) {
            Field[] fields = currentClass.getDeclaredFields();
            fieldsList.addAll(Arrays.asList(fields));
            Class<?> superclass = currentClass.getSuperclass();
            if (!superclass.equals(currentClass) && Service.class.isAssignableFrom(superclass))
                currentClass = superclass;
            else currentClass = null;
        }
        Field[] fields = new Field[fieldsList.size()];
        return fieldsList.toArray(fields);
    }
}
