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

import io.fabric8.kubernetes.client.informers.SharedInformerEventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Haytham Mohamed
 **/
public class EndpointsEventListener implements SharedInformerEventListener {

	private static Logger logger = LoggerFactory.getLogger(EndpointsEventListener.class);

	@Override
	public void onException(Exception e) {
		logger.error("Exception Occur: " + e.getMessage());
	}
}
