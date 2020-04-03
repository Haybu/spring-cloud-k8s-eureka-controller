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

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

import io.agilehandy.k8s.common.CommonInformerProperties;
import io.agilehandy.k8s.common.CommonUtil;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.informers.cache.Lister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class EndpointsCache implements InitializingBean {

	private static Logger logger = LoggerFactory.getLogger(EndpointsCache.class);

	// set of resources UID
	private final Set<String> cache = new ConcurrentSkipListSet();

	private final KubernetesClient client;
	private final CommonInformerProperties properties;
	private final Lister<Endpoints> lister;

	private boolean synced;

	public EndpointsCache(KubernetesClient client
			, CommonInformerProperties properties
			, @Qualifier("endpointsLister") Lister<Endpoints> lister) {
		this.client = client;
		this.properties = properties;
		this.lister = lister;
		synced = false;
	}

	public boolean isSynced() { return synced; }

	public boolean exists(Endpoints ep) {
		return cache.contains(ep.getMetadata().getUid());
	}

	public boolean same(Endpoints oldep, Endpoints newep) {
		return oldep.getMetadata().getUid().equals(newep.getMetadata().getUid());
	}

	public boolean isUpdated(Endpoints oldep, Endpoints newep) {
		return !oldep.getMetadata().getResourceVersion()
				.equals(newep.getMetadata().getResourceVersion());
	}

	public boolean updateExisting(Endpoints oldep, Endpoints newep) {
		return same(oldep, newep)
				&& isUpdated(oldep, newep)
				&& exists(oldep) ;
	}

	public boolean pollingExisting(Endpoints oldep, Endpoints newep) {
		return same(oldep, newep)
				&& !isUpdated(oldep, newep)
				&& exists(oldep);
	}

	public void removeFromCache(Endpoints ep) {
		logger.info("remove from cache: " + ep.getMetadata().getUid());
		cache.remove(ep.getMetadata().getUid());
	}

	public void addToCache(Endpoints ep) {
		logger.info("add to cache: " + ep.getMetadata().getUid());
		cache.add(ep.getMetadata().getUid());
	}

	public void replace(Endpoints oldep, Endpoints newep) {
		removeFromCache(oldep);
		addToCache(newep);
	}

	// TODO: remove
	//  pre-filling the cache is not needed as the ADD event would add the endpoints resources anyways
	@Override
	public void afterPropertiesSet() throws Exception {
		logger.info("Start syncing cache.");
		lister.list()
				.stream()
				.filter(ep ->
						CommonUtil.isEnabledLabel(ep.getMetadata(), properties.getLabelEnabled())
						&&
						CommonUtil.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())
				)
				.forEach(ep -> cache.add(ep.getMetadata().getUid()))
		;
		logger.info("Cache is synced with {} endpoints", cache.size());
		synced = true;
	}
}
