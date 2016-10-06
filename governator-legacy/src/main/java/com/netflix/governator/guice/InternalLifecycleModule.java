/*
 * Copyright 2012 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package com.netflix.governator.guice;

import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;

class InternalLifecycleModule extends AbstractModule implements ProvisionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalLifecycleModule.class);
    private final AtomicReference<LifecycleManager> lifecycleManager;

    InternalLifecycleModule(AtomicReference<LifecycleManager> lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public void configure() {
        bindListener(
                Matchers.any(),
                this);      
    }

    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        T instance = provision.provision();
        if (instance != null) {
            Binding<T> binding = provision.getBinding();

            LifecycleManager manager = lifecycleManager.get();
            if (manager != null) {
                Key<T> bindingKey = binding.getKey();
                LOGGER.trace("provisioning instance of {}", bindingKey);
                TypeLiteral<T> bindingType = bindingKey.getTypeLiteral();
                for (LifecycleListener listener : manager.getListeners()) {
                    listener.objectInjected(bindingType, instance);
                }
                
                try {
                    LifecycleMethods lifecycleMethods = manager.getMetadataFunction().apply(instance.getClass());
                    if (lifecycleMethods.hasLifecycleAnnotations()) {
                            manager.add(instance, binding, lifecycleMethods);                       
                    }
                } catch (Throwable e) {
                    // unknown problem will abort injector start up
                    throw new Error(e);
                }                
            }
        }
    }
}
