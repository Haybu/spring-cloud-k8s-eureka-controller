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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import com.netflix.appinfo.InstanceInfo;
import io.agilehandy.k8s.common.InformerProperties;
import io.agilehandy.k8s.common.Util;
import io.agilehandy.k8s.eureka.Application;
import io.agilehandy.k8s.eureka.Eureka;
import io.fabric8.kubernetes.api.model.EndpointAddress;
import io.fabric8.kubernetes.api.model.EndpointPort;
import io.fabric8.kubernetes.api.model.EndpointSubset;
import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.informers.ResourceEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Haytham Mohamed
 **/

@Component
public class EndpointsEventHandler implements ResourceEventHandler<Endpoints> {

	private static Logger logger = LoggerFactory.getLogger(EndpointsEventHandler.class);

	private final Eureka lite;

	private final InformerProperties properties;
	private final EndpointsCache cache;

	public EndpointsEventHandler(Eureka lite, InformerProperties properties, EndpointsCache cache) {
		this.lite = lite;
		this.properties = properties;
		this.cache = cache;
	}

	@Override
	public void onAdd(Endpoints ep) {
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelEnabled())
				&& !cache.exists(ep)
		) {
			logger.info("Add Endpoint Action -> {}", logEndpoints(ep, false));
			cache.addToCache(ep);
		}
	}

	@Override
	public void onUpdate(Endpoints oldep, Endpoints newep) {
		if (Util.isEnabledLabel(oldep.getMetadata(), properties.getLabelEnabled())
				&& Util.isEnabledLabel(newep.getMetadata(), properties.getLabelEnabled())
				&& cache.exists(oldep)
		) {
			logger.info("Update Endpoint Action (old) -> {}", logEndpoints(oldep, true));
			logger.info("Update Endpoint Action (new) -> {}", logEndpoints(newep, true));
			if (cache.updateExisting(oldep, newep)) {
				register(newep);
			} else if (cache.pollingExisting(oldep, newep)){
				renewLease(newep);
			}
			//cache.replace(oldep, newep);
		}
	}

	@Override
	public void onDelete(Endpoints ep, boolean deletedFinalStateUnknown) {
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelEnabled())
				&& cache.exists(ep)
		) {
			logger.info("Delete Endpoint Action -> {}", logEndpoints(ep, true));
			unregister(ep);
			cache.removeFromCache(ep);
		}
	}

	private void register(Endpoints ep) {
		logger.info("handler::registering " + ep.getMetadata().getName()
				+ " with uid " + ep.getMetadata().getUid()
				+ " and version " + ep.getMetadata().getResourceVersion());
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())) {
			this.getApplications(ep).stream().forEach(this.lite::register);
		} else {
			logger.info("service registration label is disabled");
		}
	}

	private void unregister(Endpoints ep) {
		logger.info("handler::de-registering " + ep.getMetadata().getName()
				+ " with uid " + ep.getMetadata().getUid()
				+ " and version " + ep.getMetadata().getResourceVersion());
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())) {
			this.getApplications(ep)
					.stream()
					.forEach(app -> this.lite.cancel(app.getName(), app.getInstance_id()));
		} else {
			logger.info("service registration label is disabled");
		}
	}

	private void renewLease(Endpoints ep) {
		logger.info("handler::re-registering " + ep.getMetadata().getName()
				+ " with uid " + ep.getMetadata().getUid()
				+ " and version " + ep.getMetadata().getResourceVersion());
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())) {
			this.getInstancesInfo(ep).stream().forEach(lite::renew);
		} else {
			logger.info("service registration label is disabled");
		}
	}

	private List<InstanceInfo> getInstancesInfo(Endpoints ep) {
		return this.getApplications(ep).stream()
				.map(lite::getInstanceInfo)
				.collect(Collectors.toList());
	}

	// construct applications to use with Eureka lite API
	private List<Application> getApplications(Endpoints ep) {
		List<EndpointSubset> subsetsList = ep.getSubsets();
		List<Application> applications = new ArrayList();
		if (!subsetsList.isEmpty()) {
			for (EndpointSubset subset : subsetsList) {
				List<EndpointAddress> addresses = subset.getAddresses();
				for (EndpointAddress endpointAddress : addresses) {
					logger.info("endpointAddress: {}", endpointAddress.toString());
					String instanceId = null;
					if (endpointAddress.getTargetRef() != null) {
						instanceId = endpointAddress.getTargetRef().getUid();
					}

					// TODO: check and error handling if instanceId is null

					EndpointPort endpointPort = this.findEndpointPort(subset);
					applications.add(new Application(ep.getMetadata().getName(),
							instanceId, endpointAddress.getIp(), endpointPort.getPort()));
				}
			}
		} else {
			logger.info("subsets are not available now for endpoint {} (pods may not be created yet)"
					, ep.getMetadata().getName()
			);
		}

		return applications;
	}

	// Find first defined endPoint port.
	// If a primary port is defined it would be considered
	// leveraging function in spring-cloud-kubernetes project
	private EndpointPort findEndpointPort(EndpointSubset s) {
		List<EndpointPort> ports = s.getPorts();
		EndpointPort endpointPort;
		if (ports.size() == 1) {
			endpointPort = ports.get(0);
		}
		else {
			Predicate<EndpointPort> portPredicate;
			if (!StringUtils.isEmpty(properties.getPrimaryPortName())) {
				portPredicate = port ->
						properties.getPrimaryPortName().equalsIgnoreCase(port.getName());
			}
			else {
				portPredicate = port -> true;
			}
			endpointPort = ports.stream().filter(portPredicate).findAny()
					.orElseThrow(IllegalStateException::new);
		}
		return endpointPort;
	}

	// ------------ for logging purpose ---------------------
	private String logEndpoints(Endpoints ep, boolean extraInfo) {
		StringBuilder sb = new StringBuilder("namespace: "  + ep.getMetadata().getNamespace());
		sb.append(", name: " + ep.getMetadata().getName());
		sb.append(", version: " + ep.getMetadata().getResourceVersion());
		sb.append(", uid: " + ep.getMetadata().getUid());
		if (extraInfo) {
			Map<String, Map<String, String>> map = this.endpointInstanceIds(ep);
			map.entrySet().stream().forEach(entry -> {
				String serviceName = entry.getKey();
				sb.append(", serviceName: " + serviceName);
				Map<String, String> instances = entry.getValue();
				instances.entrySet().stream().forEach(e -> {
					String instanceId = e.getKey();
					String hostPort = e.getValue();
					sb.append(", [serviceId: " + instanceId);
					sb.append(", host:port: " + hostPort + "]");
				});
			});
		}
		return sb.toString();
	}

	// <servicename: Map<instanceId: host:port>>
	private Map<String, Map<String, String>> endpointInstanceIds(Endpoints ep) {
		Map<String, String> instanceIds = new HashMap<>();
		Map<String, Map<String, String>> signature = new HashMap<>();
		List<EndpointSubset> subsetsList = ep.getSubsets();

		if (!subsetsList.isEmpty()) {
			for (EndpointSubset subset : subsetsList) {
				List<EndpointAddress> addresses = subset.getAddresses();
				for (EndpointAddress endpointAddress : addresses) {
					String instanceId = null;
					if (endpointAddress.getTargetRef() != null) {
						instanceId = endpointAddress.getTargetRef().getUid();
					}
					EndpointPort endpointPort = this.findEndpointPort(subset);
					instanceIds.put(instanceId
							, endpointAddress.getIp()+":"+endpointPort.getPort());
				}
			}
			signature.put(ep.getMetadata().getName(), instanceIds);
		}
		else {
			logger.info("subsets are not available now for endpoint {} (pods may not be created yet)"
						, ep.getMetadata().getName()
			);
		}
		return signature;
	}

}
