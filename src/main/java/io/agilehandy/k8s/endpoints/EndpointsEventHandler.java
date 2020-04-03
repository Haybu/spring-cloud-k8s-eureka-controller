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

import io.agilehandy.k8s.common.InformerProperties;
import io.agilehandy.k8s.common.Util;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class EndpointsEventHandler implements ResourceEventHandler<Endpoints> {

	private static Logger logger = LoggerFactory.getLogger(EndpointsEventHandler.class);

	private final EndpointsEureka registrar;
	private final InformerProperties properties;
	private final EndpointsCache cache;

	public EndpointsEventHandler(EndpointsEureka registrar, InformerProperties properties, EndpointsCache cache) {
		this.registrar = registrar;
		this.properties = properties;
		this.cache = cache;
	}

	@Override
	public void onAdd(Endpoints ep) {
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelEnabled())
				&& !cache.exists(ep)
		) {
			logger.debug("Add Endpoint Action -> {}", log(ep));
			cache.addToCache(ep);
		}
	}

	@Override
	public void onUpdate(Endpoints oldep, Endpoints newep) {
		if (Util.isEnabledLabel(oldep.getMetadata(), properties.getLabelEnabled())
				&& Util.isEnabledLabel(newep.getMetadata(), properties.getLabelEnabled())
				&& cache.exists(oldep)
		) {
			logger.debug("Update Endpoint Action (old) -> {}", log(oldep));
			logger.debug("Update Endpoint Action (new) -> {}", log(newep));
			if (cache.updateExisting(oldep, newep)) {
				registrar.register(newep);
			} else if (cache.pollingExisting(oldep, newep)){
				registrar.renewLease(newep);
			}
		}
	}

	@Override
	public void onDelete(Endpoints ep, boolean deletedFinalStateUnknown) {
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelEnabled())
				&& cache.exists(ep)
		) {
			logger.debug("Delete Endpoint Action -> {}", log(ep));
			registrar.unregister(ep);
			cache.removeFromCache(ep);
		}
	}

	private String log(Endpoints ep) {
		StringBuilder sb = new StringBuilder("name: "  + ep.getMetadata().getName());
		sb.append(", namespace: " + ep.getMetadata().getNamespace());
		sb.append(", uid: " + ep.getMetadata().getUid());
		sb.append(", version: " + ep.getMetadata().getResourceVersion());
		return sb.toString();
	}

}
