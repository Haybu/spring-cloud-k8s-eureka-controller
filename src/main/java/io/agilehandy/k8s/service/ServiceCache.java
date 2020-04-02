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

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import javax.annotation.PostConstruct;

import io.agilehandy.k8s.common.CommonInformerProperties;
import io.agilehandy.k8s.common.CommonUtil;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class ServiceCache {

	private static Logger logger = LoggerFactory.getLogger(ServiceCache.class);

	// set of resourceVersion
	private final Set<String> cache = new ConcurrentSkipListSet();

	private final KubernetesClient client;
	private final CommonInformerProperties properties;
	private final Lister<Service> lister;

	private boolean synced;

	public ServiceCache(KubernetesClient client, CommonInformerProperties properties, Lister<Service> lister) {
		this.client = client;
		this.properties = properties;
		this.lister = lister;
		synced = false;
	}

	public boolean isSynced() { return synced; }

	public boolean exists(Service service) {
		return cache.contains(service.getMetadata().getResourceVersion());
	}

	public boolean updatedServiceInCache(Service oldservice, Service newservice) {
		return oldservice.equals(newservice)
				&& exists(oldservice) && exists(newservice);
	}

	public void removeFromCache(Service service) {
		cache.remove(service.getMetadata().getResourceVersion());
	}

	public void addToCache(Service service) {
		cache.add(service.getMetadata().getResourceVersion());
	}

	@PostConstruct
	private void boostrapCache() {
		logger.debug("Start syncing cache.");
		lister.list()
				.stream()
				.filter(service -> CommonUtil.isEnabledLabel(service.getMetadata(), properties.getLabelEnabled()))
				.forEach(service -> cache.add(service.getMetadata().getResourceVersion()))
		;
		logger.debug("Cache is sync with {} services", cache.size());
		synced = true;
	}

}
