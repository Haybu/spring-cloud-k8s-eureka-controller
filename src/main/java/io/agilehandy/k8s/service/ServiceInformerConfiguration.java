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
package io.agilehandy.k8s.service;

import io.agilehandy.k8s.common.CommonInformerProperties;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.api.model.ServiceList;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import io.fabric8.kubernetes.client.informers.cache.Lister;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Haytham Mohamed
 **/

@Configuration
public class ServiceInformerConfiguration {

	private final CommonInformerProperties properties;

	public ServiceInformerConfiguration(CommonInformerProperties properties) {
		this.properties = properties;
	}

	@Bean(name="serviceSharedIndexInfromer")
	public SharedIndexInformer<Service> serviceSharedIndexInformer(SharedInformerFactory factory) {
		return factory.sharedIndexInformerFor(Service.class
				, ServiceList.class
				, properties.getWatcherInterval() * 1000L);
	}

	@Bean(name="serviceLister")
	public Lister<Service> serviceLister(SharedIndexInformer<Service> informer
			, KubernetesClient client) {
		return new Lister(informer.getIndexer()
				, client.getNamespace());
	}

}
