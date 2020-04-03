/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.agilehandy.k8s.endpoints;

import io.agilehandy.k8s.common.CommonInformerProperties;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.api.model.EndpointsList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.kubernetes.client.informers.cache.Lister;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Haytham Mohamed
 **/

@Configuration
public class EndpointsInformerConfiguration {

	@Bean(name="endpointsSharedInformer")
	public SharedIndexInformer<Endpoints> serviceSharedIndexInformer(CommonInformerProperties properties,
			SharedInformerFactory factory) {
		return factory.sharedIndexInformerFor(Endpoints.class, EndpointsList.class
				, properties.getWatcherInterval() * 1000L);
	}

	@Bean(name="endpointsLister")
	public Lister<Endpoints> serviceLister(@Qualifier("endpointsSharedInformer") SharedIndexInformer<Endpoints> informer
			, KubernetesClient client) {
		return new Lister(informer.getIndexer(), client.getNamespace());
	}

}
