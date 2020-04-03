package io.agilehandy.k8s.eureka;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Applications;
import com.netflix.discovery.shared.resolver.ClosableResolver;
import com.netflix.discovery.shared.resolver.EndpointRandomizer;
import com.netflix.discovery.shared.resolver.EurekaEndpoint;
import com.netflix.discovery.shared.resolver.aws.AwsEndpoint;
import com.netflix.discovery.shared.transport.EurekaHttpClient;
import com.netflix.discovery.shared.transport.EurekaHttpClientFactory;
import com.netflix.discovery.shared.transport.EurekaHttpClients;
import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.discovery.shared.transport.EurekaTransportConfig;
import com.netflix.discovery.shared.transport.TransportClientFactory;
import com.netflix.discovery.shared.transport.decorator.MetricsCollectingEurekaHttpClient;
import com.netflix.discovery.shared.transport.jersey.JerseyEurekaHttpClientFactory;
import com.sun.jersey.api.client.filter.ClientFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.BeansException;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.cloud.netflix.eureka.EurekaClientConfigBean;
import org.springframework.cloud.netflix.eureka.EurekaInstanceConfigBean;
import org.springframework.cloud.netflix.eureka.InstanceInfoFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.http.HttpStatus;

/**
 * @author Spencer Gibb
 */
public class Eureka implements ApplicationContextAware {

	private static Logger logger = LoggerFactory.getLogger(Eureka.class);

	private InetUtils inetUtils;
	private CloudEurekaClient eurekaClient;
	private ApplicationContext context;
	private EurekaClientConfigBean clientConfig;
	private EurekaTransport transport;

	public Eureka(InetUtils inetUtils, CloudEurekaClient eurekaClient, EurekaClientConfigBean clientConfig) {
		this.inetUtils = inetUtils;
		this.eurekaClient = eurekaClient;
		this.clientConfig = clientConfig;
		this.clientConfig.setRegisterWithEureka(false); // turn off registering with eureka, let apps send heartbeats.
		this.transport = createTransport();
	}

	public InstanceInfo register(Application application) {
		long start = System.currentTimeMillis();
		logger.debug("Starting registration of {}", application);
		InstanceInfo instanceInfo = getInstanceInfo(application);

		long duration = (System.currentTimeMillis() - start) ;
		logger.debug("Created registration for {} in {} ms", application, duration);

		register(instanceInfo);

		return instanceInfo;
	}

	public InstanceInfo getInstanceInfo(Registration registration) {
		InstanceInfo instanceInfo = getInstanceInfo(registration.getApplication());
		instanceInfo = new InstanceInfo.Builder(instanceInfo)
				.setLastDirtyTimestamp(registration.getInstance().getLastDirtyTimestamp())
				.setLastUpdatedTimestamp(registration.getInstance().getLastUpdatedTimestamp())
				.build();
		return instanceInfo;
	}

	public InstanceInfo getInstanceInfo(Application application) {
		EurekaInstanceConfigBean instanceConfig = new EurekaInstanceConfigBean(inetUtils);
		instanceConfig.setInstanceEnabledOnit(true);
		instanceConfig.setAppname(application.getName());
		instanceConfig.setVirtualHostName(application.getName());
		instanceConfig.setInstanceId(application.getInstance_id());
		instanceConfig.setHostname(application.getHostname());
		instanceConfig.setNonSecurePort(application.getPort());

		return new InstanceInfoFactory().create(instanceConfig);
	}

	public EurekaTransport createTransport() {
		TransportClientFactory transportClientFactory = newTransportClientFactory(clientConfig, Collections.<ClientFilter>emptyList());
		EurekaTransportConfig transportConfig = clientConfig.getTransportConfig();

		ClosableResolver<AwsEndpoint> bootstrapResolver = EurekaHttpClients.newBootstrapResolver(
				clientConfig,
				transportConfig,
				transportClientFactory,
				null,
				(stalenessThreshold, timeUnit) -> {
					long thresholdInMs = TimeUnit.MILLISECONDS.convert(stalenessThreshold, timeUnit);
					long delay = eurekaClient.getLastSuccessfulRegistryFetchTimePeriod();
					if (delay > thresholdInMs) {
						logger.info("Local registry is too stale for local lookup. Threshold:{}, actual:{}",
								thresholdInMs, delay);
						return null;
					} else {
						return eurekaClient.getApplications();
					}
				},
				new EndpointRandomizer() {  // @TODO: check this out if needed
					@Override
					public <T extends EurekaEndpoint> List<T> randomize(List<T> list) {
						return list;
					}
				}
		);

		EurekaHttpClientFactory httpClientFactory;
		try {
			httpClientFactory = EurekaHttpClients.registrationClientFactory(
					bootstrapResolver,
					transportClientFactory,
					transportConfig
			);
		} catch (Exception e) {
			logger.warn("Experimental transport initialization failure", e);
			throw new RuntimeException(e);
		}

		return new EurekaTransport(httpClientFactory, httpClientFactory.newClient(), transportClientFactory, bootstrapResolver);
	}

	public static TransportClientFactory newTransportClientFactory(final EurekaClientConfigBean clientConfig,
																   final Collection<ClientFilter> additionalFilters
																   ) {
		final TransportClientFactory jerseyFactory = JerseyEurekaHttpClientFactory.create(
				clientConfig, additionalFilters, null, null);
		final TransportClientFactory metricsFactory = MetricsCollectingEurekaHttpClient.createFactory(jerseyFactory);

		return new TransportClientFactory() {
			@Override
			public EurekaHttpClient newClient(EurekaEndpoint serviceUrl) {
				logger.info("serviceUrl: " + serviceUrl.getServiceUrl());
				return metricsFactory.newClient(serviceUrl);
			}

			@Override
			public void shutdown() {
				metricsFactory.shutdown();
				jerseyFactory.shutdown();
			}
		};
	}

	/**
	 * Renew with the eureka service by making the appropriate REST call
	 */
	public boolean renew(InstanceInfo instanceInfo) {
		EurekaHttpResponse<InstanceInfo> httpResponse;
		try {
			httpResponse = this.transport.getEurekaHttpClient().sendHeartBeat(instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo, null);
			logger.debug("EurekaLite_{}/{} - Heartbeat status: {}", instanceInfo.getAppName(), instanceInfo.getId(), httpResponse.getStatusCode());
			if (httpResponse.getStatusCode() == HttpStatus.NOT_FOUND.value()) {
				logger.info("EurekaLite_{}/{} - Re-registering apps/{}", instanceInfo.getAppName(), instanceInfo.getId(), instanceInfo.getAppName());
				return register(instanceInfo);
			}
			return httpResponse.getStatusCode() == HttpStatus.OK.value();
		} catch (Exception e) {
			logger.error("EurekaLite_"+instanceInfo.getAppName()+"/"+ instanceInfo.getId() + " - was unable to send heartbeat!", e);
			return false;
		}
	}

	/**
	 * Register with the eureka service by making the appropriate REST call.
	 */
	protected boolean register(InstanceInfo instanceInfo) {
		logger.info("EurekaLite_{}/{}: registering service...", instanceInfo.getAppName(), instanceInfo.getId());
		EurekaHttpResponse<Void> httpResponse;
		try {
			httpResponse = this.transport.getEurekaHttpClient().register(instanceInfo);
		} catch (Exception e) {
			logger.warn("EurekaLite_"+instanceInfo.getAppName()+"/"+ instanceInfo.getId() + " - registration failed " + e.getMessage(), e);
			throw e;
		}
		if (logger.isInfoEnabled()) {
			logger.info("EurekaLite_{}/{} - registration status: {}", instanceInfo.getAppName(), instanceInfo.getId(), httpResponse.getStatusCode());
		}
		return httpResponse.getStatusCode() == HttpStatus.NO_CONTENT.value();
	}

	public void cancel(String appName, String instanceId) {
		try {
			EurekaHttpResponse<Void> httpResponse = this.transport.getEurekaHttpClient().cancel(appName, instanceId);
			logger.info("EurekaLite_{}/{} - deregister  status: {}", appName, instanceId, httpResponse.getStatusCode());
		} catch (Exception e) {
			logger.error("EurekaLite_"+appName+"/"+ instanceId + " - de-registration failed " + e.getMessage(), e);
		}
		this.transport.shutdown();
	}

	public Registration getRegistration(String appName, String instanceId) {
		EurekaHttpResponse<InstanceInfo> response = this.transport.getEurekaHttpClient().getInstance(appName, instanceId);
		//TODO: error handling and logging
		InstanceInfo instanceInfo = response.getEntity();

		Registration registration = getRegistration(instanceInfo);
		return registration;
	}

	protected Registration getRegistration(InstanceInfo instanceInfo) {
		Application application = new Application(instanceInfo.getAppName(), instanceInfo.getInstanceId(), instanceInfo.getHostName(), instanceInfo.getPort());

		Registration registration = new Registration();
		registration.setApplication(application);
		registration.update(instanceInfo);
		return registration;
	}

	public List<Registration> getRegistrations(String appName) {
		EurekaHttpResponse<com.netflix.discovery.shared.Application> response = this.transport.getEurekaHttpClient().getApplication(appName);
		//TODO: error handling and logging
		com.netflix.discovery.shared.Application application = response.getEntity();

		return getRegistrations(application);
	}

	protected List<Registration> getRegistrations(com.netflix.discovery.shared.Application application) {
		ArrayList<Registration> registrations = new ArrayList<>();
		for (InstanceInfo instanceInfo : application.getInstances()) {
			registrations.add(getRegistration(instanceInfo));
		}
		return registrations;
	}

	public Map<String, List<Registration>> getApplications() {
		//TODO: support regions
		EurekaHttpResponse<Applications> response = this.transport.getEurekaHttpClient().getApplications();
		List<com.netflix.discovery.shared.Application> applications = response.getEntity().getRegisteredApplications();
		LinkedHashMap<String, List<Registration>> map = new LinkedHashMap<>();

		for (com.netflix.discovery.shared.Application application : applications) {
			List<Registration> registrations = getRegistrations(application);
			map.put(application.getName(), registrations);
		}

		return map;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		context = applicationContext;
	}
}
