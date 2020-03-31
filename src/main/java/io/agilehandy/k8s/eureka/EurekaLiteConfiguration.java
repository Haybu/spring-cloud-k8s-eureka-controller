/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.agilehandy.k8s.eureka;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

/**
 * @author Spencer Gibb
 */
@Configuration
@EnableConfigurationProperties
public class EurekaLiteConfiguration {
	//@Bean
	public EurekaLiteProperties eurekaLiteProperties() {
		return new EurekaLiteProperties();
	}

	@Bean
	@ConditionalOnMissingBean
	public Eureka eureka(InetUtils inetUtils, @Lazy CloudEurekaClient eurekaClient) {
		return new Eureka(inetUtils, eurekaClient);
	}

	//@Bean
	public EurekaLiteController eurekaLiteController(Eureka eureka, EurekaLiteProperties properties) {
		return new EurekaLiteController(eureka, properties);
	}

}
