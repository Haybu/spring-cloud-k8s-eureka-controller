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

import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.Service;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class ServiceInformer implements ApplicationRunner {

	private static Logger logger =
			LoggerFactory.getLogger(ServiceInformer.class);

	private final ServiceEventHandler handler;
	private final SharedInformerFactory factory;

	public ServiceInformer(ServiceEventHandler handler
			, SharedInformerFactory factory) {
		this.handler = handler;
		this.factory = factory;
	}

	@PreDestroy
	public void destroy() {
		logger.info("Stopping all registered service informers");
		factory.stopAllRegisteredInformers();
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		SharedIndexInformer<Service> informer =
				factory.getExistingSharedIndexInformer(Service.class);
		informer.addEventHandler(handler);
		logger.info("Starting all registered service informers");
		factory.startAllRegisteredInformers();
	}
}
