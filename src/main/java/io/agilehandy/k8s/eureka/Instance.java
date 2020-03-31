package io.agilehandy.k8s.eureka;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.netflix.appinfo.InstanceInfo;

import org.springframework.validation.annotation.Validated;

/**
 * @author Spencer Gibb
 */

@Validated
public class Instance {
	@NotNull
	private InstanceInfo.InstanceStatus status;

	@Min(1)
	private long lastDirtyTimestamp;

	@Min(1)
	private long lastUpdatedTimestamp;

	public Instance() {}

	public Instance(Instance other) {
		this.status = other.getStatus();
		this.lastDirtyTimestamp = other.getLastDirtyTimestamp();
		this.lastUpdatedTimestamp = other.getLastUpdatedTimestamp();
	}

	public Instance(@NotNull InstanceInfo.InstanceStatus status
			, @Min(1) long lastDirtyTimestamp
			, @Min(1) long lastUpdatedTimestamp) {
		this.status = status;
		this.lastDirtyTimestamp = lastDirtyTimestamp;
		this.lastUpdatedTimestamp = lastUpdatedTimestamp;
	}

	public void update(InstanceInfo instanceInfo) {
		if (instanceInfo == null) {
			//TODO: log warning
			return;
		}
		setStatus(instanceInfo.getStatus());
		setLastDirtyTimestamp(instanceInfo.getLastDirtyTimestamp());
		setLastUpdatedTimestamp(instanceInfo.getLastUpdatedTimestamp());
	}

	public InstanceInfo.InstanceStatus getStatus() {
		return status;
	}

	public long getLastDirtyTimestamp() {
		return lastDirtyTimestamp;
	}

	public long getLastUpdatedTimestamp() {
		return lastUpdatedTimestamp;
	}

	public void setStatus(InstanceInfo.InstanceStatus status) {
		this.status = status;
	}

	public void setLastDirtyTimestamp(long lastDirtyTimestamp) {
		this.lastDirtyTimestamp = lastDirtyTimestamp;
	}

	public void setLastUpdatedTimestamp(long lastUpdatedTimestamp) {
		this.lastUpdatedTimestamp = lastUpdatedTimestamp;
	}

}
