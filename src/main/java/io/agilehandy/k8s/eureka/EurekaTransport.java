package io.agilehandy.k8s.eureka;

import com.netflix.discovery.shared.resolver.ClosableResolver;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpClientFactory;
import com.netflix.discovery.shared.transport.TransportClientFactory;

/**
 * @author Spencer Gibb
 */
public class EurekaTransport {

	private final EurekaHttpClientFactory eurekaHttpClientFactory;
	private final EurekaHttpClient eurekaHttpClient;
	private final TransportClientFactory transportClientFactory;
	private final ClosableResolver closableResolver;

	EurekaTransport(EurekaHttpClientFactory eurekaHttpClientFactory
			, EurekaHttpClient eurekaHttpClient
			, TransportClientFactory transportClientFactory
			, ClosableResolver closableResolver) {
		this.eurekaHttpClientFactory = eurekaHttpClientFactory;
		this.eurekaHttpClient = eurekaHttpClient;
		this.transportClientFactory = transportClientFactory;
		this.closableResolver = closableResolver;
	}

	public EurekaHttpClientFactory getEurekaHttpClientFactory() {
		return eurekaHttpClientFactory;
	}

	public EurekaHttpClient getEurekaHttpClient() {
		return eurekaHttpClient;
	}

	public TransportClientFactory getTransportClientFactory() {
		return transportClientFactory;
	}

	public ClosableResolver getClosableResolver() {
		return closableResolver;
	}

	public void shutdown() {
		eurekaHttpClientFactory.shutdown();
		eurekaHttpClient.shutdown();
		transportClientFactory.shutdown();
		closableResolver.shutdown();
	}
}
