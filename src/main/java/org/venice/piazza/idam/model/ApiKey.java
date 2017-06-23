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
package org.venice.piazza.idam.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model for representing an API Key in the Mongo Database. References the key itself, the user, and the creation and
 * expiration dates.
 * 
 * @author Patrick.Doody
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiKey {
	private String uuid;
	private String username;
	private long createdOn;
	private long expiresOn;
	private long lastUsedOn;

	public ApiKey() {
		// Empty constructor required by Jackson
	}

	/**
	 * Creates a new Key
	 * 
	 * @param apiKey
	 *            The API Key
	 * @param userName
	 *            Username of the owner
	 * @param createdOn
	 *            Epoch, time of creation
	 * @param expiresOn
	 *            Epoch, time of expiration
	 */
	public ApiKey(String apiKey, String userName, long createdOn, long expiresOn) {
		this.uuid = apiKey;
		this.username = userName;
		this.createdOn = createdOn;
		this.expiresOn = expiresOn;
		this.lastUsedOn = System.currentTimeMillis();
	}

	public String getUuid() {
		return uuid;
	}

	public String getUsername() {
		return username;
	}

	public long getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(long createdOn) {
		this.createdOn = createdOn;
	}

	public long getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(long expiresOn) {
		this.expiresOn = expiresOn;
	}

	public long getLastUsedOn() {
		return lastUsedOn;
	}

	public void setLastUsedOn(long lastUsedOn) {
		this.lastUsedOn = lastUsedOn;
	}
}