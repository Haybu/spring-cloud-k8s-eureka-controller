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

import io.agilehandy.k8s.common.CommonUtil;
import io.agilehandy.k8s.common.CommonInformerProperties;
import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class ServiceEventHandler implements ResourceEventHandler<Service> {

	private static Logger logger = LoggerFactory.getLogger(ServiceEventHandler.class);

	private final CommonInformerProperties properties;
	private final ServiceCache cache;

	public ServiceEventHandler(CommonInformerProperties properties, ServiceCache cache) {
		this.properties = properties;
		this.cache = cache;
	}

	@Override
	public void onAdd(Service service) {
		if (cache.isSynced()
				&& CommonUtil.isSpringLabeled(service.getMetadata(), properties.getLabelEnabled())
				&& !cache.exists(service)
		) {
			cache.addToCache(service);
			// TODO: action here
			logger.debug("{} spring Service is added", logService(service));
		}
	}

	@Override
	public void onUpdate(Service oldservice, Service newservice) {
		if (cache.isSynced()
				&& CommonUtil.isSpringLabeled(oldservice.getMetadata(), properties.getLabelEnabled())
				&& CommonUtil.isSpringLabeled(newservice.getMetadata(), properties.getLabelEnabled())
				&& !cache.updatedServiceInCache(oldservice, newservice)
		) {
			cache.removeFromCache(oldservice);
			cache.addToCache(newservice);
			// TODO: action here
			logger.debug("{} spring Service is updated", logService(oldservice));
		}
	}

	@Override
	public void onDelete(Service service, boolean deletedFinalStateUnknown) {
		if (cache.isSynced()
				&& CommonUtil.isSpringLabeled(service.getMetadata(), properties.getLabelEnabled())
				&& cache.exists(service)
		) {
			cache.removeFromCache(service);
			// TODO: action here
			logger.debug("{} spring Service is deleted", logService(service));
		}
	}

	private String logService(Service service) {
		StringBuilder sb = new StringBuilder("namespace: "  + service.getMetadata().getNamespace());
		sb.append("\tname: " + service.getMetadata().getName());
		return sb.toString();
	}

}
