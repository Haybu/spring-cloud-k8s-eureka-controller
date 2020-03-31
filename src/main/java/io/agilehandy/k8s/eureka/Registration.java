package io.agilehandy.k8s.eureka;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import com.netflix.appinfo.InstanceInfo;

import org.springframework.validation.annotation.Validated;

/**
 * @author Spencer Gibb
 */

@Validated
public class Registration {

	@NotNull
	@Valid
	private Application application;

	@NotNull
	@Valid
	private Instance instance;

	public Registration() {}

	public Registration(Registration other) {
		this.application = other.getApplication();
		this.instance = other.getInstance();
	}

	public Registration(@NotNull @Valid Application application
			, @NotNull @Valid Instance instance) {
		this.application = application;
		this.instance = instance;
	}

	//TODO: refactor
	public void update(InstanceInfo instanceInfo) {
		this.instance.update(instanceInfo);
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public Instance getInstance() {
		return instance;
	}

	public void setInstance(Instance instance) {
		this.instance = instance;
	}
}
