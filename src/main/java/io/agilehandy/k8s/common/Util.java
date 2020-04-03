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

import io.fabric8.kubernetes.api.model.ObjectMeta;

/**
 * @author Haytham Mohamed
 **/
public class Util {

	static public boolean isEnabledLabel(ObjectMeta metadata, String label) {
		String cfg = metadata.getLabels() != null?
				metadata.getLabels().containsKey(label)?
						metadata.getLabels().get(label):"false"
				: "false"
				;
		return Boolean.valueOf(cfg.toLowerCase()).booleanValue();
	}

}
