/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.venice.piazza.idam.model.user;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import model.security.authz.Throttle;
import model.security.authz.Throttle.Component;

/**
 * Throttle metadata for a user that tracks that users activity with Piazza jobs. Will keep a record count of all Piazza
 * Jobs a user has performed in the last period of activity. This user is collected in order to determine if a user
 * should eventually be throttled or not due to excessive activity.
 * 
 * @author Patrick.Doody
 *
 */
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserThrottles {
	public String username;
	/**
	 * Maps a Throttle component to the number of invocations for that component.
	 */
	public Map<String, Integer> throttles = new HashMap<String, Integer>();

	public UserThrottles() {
		// Currently, there are three types of throttles per component. Automatically create this based on the enum in
		// the Throttle. Create a map entry for each and set the current count to zero.
		for (Component component : Throttle.Component.values()) {
			throttles.put(component.toString(), 0);
		}
	}

	public UserThrottles(String username) {
		this();
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Map<String, Integer> getThrottles() {
		return throttles;
	}

	public void setThrottles(Map<String, Integer> throttles) {
		this.throttles = throttles;
	}

	/**
	 * Gets the number of invocations for the specified component
	 * 
	 * @param component
	 *            The component
	 * @return The number of invocations
	 */
	public Integer getInvocations(Component component) {
		return throttles.get(component.toString());
	}
}
