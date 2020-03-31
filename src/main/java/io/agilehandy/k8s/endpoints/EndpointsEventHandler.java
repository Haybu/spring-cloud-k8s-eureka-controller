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

import com.netflix.appinfo.InstanceInfo;
import io.agilehandy.k8s.common.CommonInformerProperties;
import io.agilehandy.k8s.common.CommonUtil;
import io.agilehandy.k8s.eureka.Application;
import io.agilehandy.k8s.eureka.Eureka;
import io.agilehandy.k8s.eureka.Registration;
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

	private final CommonInformerProperties properties;
	private final EndpointsCache cache;

	public EndpointsEventHandler(Eureka lite, CommonInformerProperties properties, EndpointsCache cache) {
		this.lite = lite;
		this.properties = properties;
		this.cache = cache;
	}

	@Override
	public void onAdd(Endpoints ep) {
		if (cache.isSynced()
				&& CommonUtil.isSpringLabeled(ep.getMetadata(), properties.getLabelEnabled())
				&& !cache.exists(ep)
		) {
			cache.addToCache(ep);
			logger.info("{} spring endpoint is added", logEndpoints(ep));
			this.register(ep);

		}
	}

	@Override
	public void onUpdate(Endpoints oldep, Endpoints newep) {
		if (cache.isSynced()
				&& CommonUtil.isSpringLabeled(oldep.getMetadata(), properties.getLabelEnabled())
				&& CommonUtil.isSpringLabeled(newep.getMetadata(), properties.getLabelEnabled())
				&& !cache.exists(newep)
		) {
			cache.removeFromCache(oldep);
			this.unregister(oldep);
			cache.addToCache(newep);
			this.register(newep);
			logger.info("{} spring endpoint is updated", logEndpoints(oldep));
		}
	}

	@Override
	public void onDelete(Endpoints ep, boolean deletedFinalStateUnknown) {
		if (cache.isSynced()
				&& CommonUtil.isSpringLabeled(ep.getMetadata(), properties.getLabelEnabled())
				&& cache.exists(ep)
		) {
			cache.removeFromCache(ep);
			this.unregister(ep);
			logger.info("{} spring endpoint is deleted", logEndpoints(ep));
		}
	}

	private List<Registration> register(Endpoints ep) {
		List<Application> applications = this.getApplications(ep);
		List<Registration> registrations = new ArrayList<>();
		for (Application app: applications) {
			InstanceInfo instanceInfo = this.lite.register(app);
			Registration registration = new Registration();
			registration.setApplication(app);
			registration.update(instanceInfo);
			registrations.add(registration);
		}
		return registrations;
	}

	private void unregister(Endpoints ep) {
		List<Application> applications = this.getApplications(ep);
		for (Application app: applications) {
			this.lite.cancel(app.getName(), app.getInstance_id());
		}
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
		}

		return applications;
	}

	// Find first defined endPoint port.
	// If a primary port is defined it would be considered
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
	private String logEndpoints(Endpoints ep) {
		StringBuilder sb = new StringBuilder("namespace: "  + ep.getMetadata().getNamespace());
		sb.append("\tendpoint name: " + ep.getMetadata().getName());
		Map<String, Map<String, String>> map = this.endpointInstanceIds(ep);
		map.entrySet().stream().forEach(entry -> {
			String serviceName = entry.getKey();
			sb.append("\tserviceName: " + serviceName);
			Map<String, String> instances = entry.getValue();
			instances.entrySet().stream().forEach(e -> {
				String instanceId = e.getKey();
				String hostPort = e.getValue();
				sb.append("\t[serviceId: " + instanceId);
				sb.append("\thost:port: " + hostPort + "]");
			});
		});
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
		return signature;
	}

}
