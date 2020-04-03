package io.agilehandy.k8s;

import io.agilehandy.k8s.common.InformerProperties;
import io.agilehandy.k8s.endpoints.EndpointsInformer;
import io.agilehandy.k8s.eureka.EurekaLiteProperties;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableConfigurationProperties(value={InformerProperties.class
		, EurekaLiteProperties.class, EurekaClientConfigBean.class})
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

	@Bean
	public ApplicationRunner runner(EndpointsInformer endpointsInformer) {
		return args -> endpointsInformer.run();
	}

}
