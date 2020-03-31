package io.agilehandy.k8s;

import io.agilehandy.k8s.common.CommonInformerProperties;
import io.agilehandy.k8s.eureka.EurekaLiteProperties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(value={CommonInformerProperties.class, EurekaLiteProperties.class})
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication.run(MainApplication.class, args);
	}

}
