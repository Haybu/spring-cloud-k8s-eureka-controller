package io.agilehandy.k8s.eureka;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @author Spencer Gibb
 */

@ConfigurationProperties("eureka.lite")
public class EurekaLiteProperties {

	private boolean unregisterOnShutdown = false;

	public boolean isUnregisterOnShutdown() {
		return unregisterOnShutdown;
	}

	public void setUnregisterOnShutdown(boolean unregisterOnShutdown) {
		this.unregisterOnShutdown = unregisterOnShutdown;
	}
}
