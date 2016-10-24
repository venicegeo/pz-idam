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

/**
 * User-specific properties and metadata
 * 
 * @author Patrick.Doody
 *
 */
public class UserProperties {
	private String username;
	private Boolean isActive;
	private Map<String, String> thirdPartyKeys = new HashMap<String, String>();
	// Some container for metadata, such as contact information? GeoAxis or Here?

	public UserProperties(String username) {
		this.username = username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean isActive) {
		this.isActive = isActive;
	}

	public Map<String, String> getThirdPartyKeys() {
		return thirdPartyKeys;
	}
}
