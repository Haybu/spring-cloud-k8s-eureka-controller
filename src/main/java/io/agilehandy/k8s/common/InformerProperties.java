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
package io.agilehandy.k8s.common;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Haytham Mohamed
 **/

@ConfigurationProperties("eureka.k8s.proxy")
public class InformerProperties {

	private int watcherInterval = 30;
	private String labelEnabled = "cloud.spring.io/enabled";
	private String labelRegister = "cloud.spring.io/register";
	private String primaryPortName = "primary_port";
	private String namespace = "default";

	public String getLabelEnabled() {
		return labelEnabled;
	}

	public void setLabelEnabled(String labelEnabled) {
		this.labelEnabled = labelEnabled;
	}

	public String getLabelRegister() {
		return labelRegister;
	}

	public void setLabelRegister(String labelRegister) {
		this.labelRegister = labelRegister;
	}

	public int getWatcherInterval() {
		return watcherInterval;
	}

	public void setWatcherInterval(int watcherInterval) {
		this.watcherInterval = watcherInterval;
	}

	public String getPrimaryPortName() {
		return primaryPortName;
	}

	public void setPrimaryPortName(String primaryPortName) {
		this.primaryPortName = primaryPortName;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}
}
