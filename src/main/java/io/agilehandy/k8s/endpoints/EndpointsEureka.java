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
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * @author Haytham Mohamed
 **/

@Component
public class EndpointsEureka {

	private static Logger logger = LoggerFactory.getLogger(EndpointsEureka.class);

	private final Eureka lite;
	private final InformerProperties properties;

	public EndpointsEureka(Eureka lite, InformerProperties properties) {
		this.lite = lite;
		this.properties = properties;
	}

	public void register(Endpoints ep) {
		logger.debug("EndPointsEureka::registering " + ep.getMetadata().getName()
				+ " with uid " + ep.getMetadata().getUid()
				+ " and version " + ep.getMetadata().getResourceVersion());
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())) {
			this.getApplications(ep).stream().forEach(this.lite::register);
		} else {
			logger.debug("service registration label is disabled");
		}
	}

	public void unregister(Endpoints ep) {
		logger.debug("EndPointsEureka::de-registering " + ep.getMetadata().getName()
				+ " with uid " + ep.getMetadata().getUid()
				+ " and version " + ep.getMetadata().getResourceVersion());
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())) {
			this.getApplications(ep)
					.stream()
					.forEach(app -> this.lite.cancel(app.getName(), app.getInstance_id()));
		} else {
			logger.debug("service registration label is disabled");
		}
	}

	public void renewLease(Endpoints ep) {
		logger.debug("EndPointsEureka::re-registering " + ep.getMetadata().getName()
				+ " with uid " + ep.getMetadata().getUid()
				+ " and version " + ep.getMetadata().getResourceVersion());
		if (Util.isEnabledLabel(ep.getMetadata(), properties.getLabelRegister())) {
			this.getInstancesInfo(ep).stream().forEach(lite::renew);
		} else {
			logger.debug("service registration label is disabled");
		}
	}

	private List<InstanceInfo> getInstancesInfo(Endpoints ep) {
		return this.getApplications(ep).stream()
				.map(lite::getInstanceInfo)
				.collect(Collectors.toList());
	}

	// construct applications to use with Eureka lite API
	public List<Application> getApplications(Endpoints ep) {
		List<EndpointSubset> subsetsList = ep.getSubsets();
		List<Application> applications = new ArrayList();
		if (!subsetsList.isEmpty()) {
			for (EndpointSubset subset : subsetsList) {
				List<EndpointAddress> addresses = subset.getAddresses();
				for (EndpointAddress endpointAddress : addresses) {
					logger.debug("endpointAddress: {}", endpointAddress.toString());
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
			logger.debug("subsets are not available now for endpoint {} (pods may not be created yet)"
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

}
