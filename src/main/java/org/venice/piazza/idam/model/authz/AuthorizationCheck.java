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
package org.venice.piazza.idam.model.authz;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.swagger.annotations.ApiModelProperty;
import model.security.authz.Permission;

/**
 * Model for incoming Authorization check requests, as used by the Authorization Controller.
 * 
 * @author Patrick.Doody
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(Include.NON_NULL)
public class AuthorizationCheck {
	@ApiModelProperty(value = "The name of the user.", required = true)
	public String username;
	@ApiModelProperty(value = "Describes the action the user wishes to take.", required = true)
	public Permission action;
	@ApiModelProperty(value = "The API Key for the user. If this is included, then authentication will be conducted on this API Key before authorization on the Permission takes place.", required = false)
	public String apiKey;

	/**
	 * Default constructor
	 */
	public AuthorizationCheck() {

	}

	@Override
	public String toString() {
		return String.format("User %s requesting Action %s", username, action != null ? action.toString() : "null");
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public Permission getAction() {
		return action;
	}

	public void setAction(Permission action) {
		this.action = action;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
}
