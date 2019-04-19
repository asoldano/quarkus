/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package io.quarkus.resteasy.client.deployment;

import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.ext.Providers;

import org.apache.commons.logging.impl.Jdk14Logger;
import org.apache.commons.logging.impl.LogFactoryImpl;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.core.providerfactory.ResteasyProviderFactoryImpl;
import org.jboss.resteasy.plugins.providers.sse.SseEventProvider;

import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.substrate.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateProxyDefinitionBuildItem;
import io.quarkus.deployment.builditem.substrate.SubstrateResourceBuildItem;

public class ResteasyClientProcessor {

    private static final String PROVIDERS_SERVICE_FILE = "META-INF/services/" + Providers.class.getName();

    @BuildStep
    @Record(ExecutionTime.STATIC_INIT)
    void build(BuildProducer<FeatureBuildItem> feature,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClass) {
        feature.produce(new FeatureBuildItem(FeatureBuildItem.RESTEASY_CLIENT));

        reflectiveClass.produce(new ReflectiveClassBuildItem(false, false,
                LogFactoryImpl.class.getName(),
                Jdk14Logger.class.getName(),
                ResteasyProviderFactoryImpl.class.getName(),
                ClientRequestFilter[].class.getName(),
                ClientResponseFilter[].class.getName(),
                javax.ws.rs.ext.ReaderInterceptor[].class.getName()));

        //register classes for reflection
        reflectiveClass.produce(new ReflectiveClassBuildItem(true, false,
                ResteasyClientBuilder.class.getName(),
                SseEventProvider.class.getName())); //to allow detection by RegisterBuiltin 
    }

    @BuildStep
    void setupProviders(BuildProducer<SubstrateResourceBuildItem> resources,
            BuildProducer<SubstrateProxyDefinitionBuildItem> proxyDefinition,
            BuildProducer<UnremovableBeanBuildItem> unremovableBeans) {

        proxyDefinition.produce(new SubstrateProxyDefinitionBuildItem("javax.ws.rs.ext.Providers"));
        resources.produce(new SubstrateResourceBuildItem(PROVIDERS_SERVICE_FILE));
        //make sure the SseEventProvider is not removed even if not referenced
        unremovableBeans.produce(new UnremovableBeanBuildItem(
                new UnremovableBeanBuildItem.BeanClassNameExclusion(SseEventProvider.class.getName())));
    }
}
