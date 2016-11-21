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

import model.security.authz.Permission;

/**
 * Model for incoming Authorization check requests, as used by the Authorization Controller.
 * 
 * @author Patrick.Doody
 *
 */
public class AuthorizationCheck {
	public String username;
	public Permission action;

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
}
