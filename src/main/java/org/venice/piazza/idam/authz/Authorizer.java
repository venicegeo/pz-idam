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
package org.venice.piazza.idam.authz;

import model.response.AuthResponse;
import model.security.authz.AuthorizationCheck;

/**
 * Interface that can be implemented by Authorization components that can either authorize or deny a specific action
 * being taken by a specific user.
 * 
 * @author Patrick.Doody
 *
 */
@FunctionalInterface
public interface Authorizer {
	/**
	 * Determines if the user can perform the specified action.
	 * 
	 * @param authorizationCheck
	 *            The authorization check, containing the username and action, and any other details, that describe an
	 *            action that a user wishes to perform.
	 * @return The Authorization details. Includes if the action is authorized or not, and optionally allows for
	 *         detailed information to be present.
	 */
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck);
}
