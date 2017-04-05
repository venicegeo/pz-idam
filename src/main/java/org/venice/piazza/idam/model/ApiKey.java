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

/**
 * Model for representing an API Key in the Mongo Database. References the key itself, the user, and the creation and
 * expiration dates.
 * 
 * @author Patrick.Doody
 *
 */
public class ApiKey {
	private String apiKey;
	private String userName;
	private Integer createdOn;
	private Integer expiresOn;

	public ApiKey() {

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
	public ApiKey(String apiKey, String userName, Integer createdOn, Integer expiresOn) {
		this.apiKey = apiKey;
		this.userName = userName;
		this.createdOn = createdOn;
		this.expiresOn = expiresOn;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}

	public String getUserName() {
		return userName;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public Integer getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Integer createdOn) {
		this.createdOn = createdOn;
	}

	public Integer getExpiresOn() {
		return expiresOn;
	}

	public void setExpiresOn(Integer expiresOn) {
		this.expiresOn = expiresOn;
	}
}
