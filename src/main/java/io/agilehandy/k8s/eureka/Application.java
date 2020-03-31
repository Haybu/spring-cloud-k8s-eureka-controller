package io.agilehandy.k8s.eureka;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonIgnore;


/**
 * @author Spencer Gibb
 */

public class Application {

	@NotBlank
	private String name;
	@NotBlank
	private String instance_id;
	@NotBlank
	private String hostname;
	@Min(0)
	@Max(65535)
	private int port;

	public Application(String name, String instance_id, String hostname, int port) {
		this.name = name;
		this.instance_id = instance_id;
		this.hostname = hostname;
		this.port = port;
	}

	public Application() {
	}


	public String getName() {
		return name;
	}

	public String getInstance_id() {
		return instance_id;
	}

	public String getHostname() {
		return hostname;
	}

	public int getPort() {
		return port;
	}

	@JsonIgnore
	public String getRegistrationKey() {
		return computeRegistrationKey(name, instance_id);
	}

	static String computeRegistrationKey(String name, String instanceId) {
		return name + ":" + instanceId;
	}
}
