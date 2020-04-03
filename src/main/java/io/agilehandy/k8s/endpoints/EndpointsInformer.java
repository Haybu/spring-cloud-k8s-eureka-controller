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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.informers.SharedIndexInformer;
import io.fabric8.kubernetes.client.informers.SharedInformerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * @author Haytham Mohamed
 **/

@Component
public class EndpointsInformer {

	private static Logger logger =
			LoggerFactory.getLogger(EndpointsInformer.class);

	private final EndpointsEventHandler handler;
	private final SharedInformerFactory factory;

	private ExecutorService executorService;

	public EndpointsInformer(EndpointsEventHandler handler
			, SharedInformerFactory factory) {
		this.handler = handler;
		this.factory = factory;
		executorService = Executors.newSingleThreadExecutor();
	}

	@PreDestroy
	public void destroy() {
		logger.debug("Stopping all registered endpoints informers");
		factory.stopAllRegisteredInformers();
		executorService.shutdown();
		logger.debug("All registered endpoints informers were stopped successfully!");
	}

	private void wait(SharedIndexInformer<Endpoints> informer) {
		try {
			executorService.submit(() -> {
				Thread.currentThread().setName("HAS_SYNCED_THREAD");
				try {
					for (;;) {
						logger.debug("informer.hasSynced() : {}", informer.hasSynced());
						Thread.sleep(1000);
					}
				} catch (InterruptedException inEx) {
					logger.debug("HAS_SYNCED_THREAD INTERRUPTED!");
				}
			});

			// Wait for some time now
			TimeUnit.MINUTES.sleep(1);
		} catch (InterruptedException interruptedException) {
			logger.debug("-> interrupted: {}", interruptedException.getMessage());
		}
	}

	public void run() {
		SharedIndexInformer<Endpoints> informer =
				factory.getExistingSharedIndexInformer(Endpoints.class);
		informer.addEventHandler(handler);
		logger.debug("Starting all registered endpoints informers");
		factory.startAllRegisteredInformers();
		wait(informer);
		logger.debug("All registered endpoints informers were started successfully!");
	}
}
