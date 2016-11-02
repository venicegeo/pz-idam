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
package org.venice.piazza.idam.authz.endpoint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.AuthResponse;
import org.venice.piazza.idam.model.authz.AuthorizationCheck;

import model.security.authz.ProfileTemplate;
import util.PiazzaLogger;

/**
 * Authorizer that determines if a specified user action will be prevented due to restricted access to a particular
 * endpoint in the Piazza API.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class EndpointAuthorizer implements Authorizer {
	@Autowired
	private MongoAccessor accessor;
	@Autowired
	private PiazzaLogger pzLogger;
	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointAuthorizer.class);

	@Override
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck) {
		// Get the Profile Template for the user requesting the action. This Template contains endpoint information.
		String username = authorizationCheck.getUsername();
		ProfileTemplate profileTemplate = getProfileTemplateForUser(username);
		// Get the appropriate Permission from the Profile Template that matches the Permission
		String keyName = authorizationCheck.getAction().getKeyName();
		Boolean canPerformAction = profileTemplate.getPermissions().get(keyName);
		if (canPerformAction) {
			return new AuthResponse(true);
		} else {
			return new AuthResponse(false, String.format("The user does not have the ability to access the %s endpoint.",
					authorizationCheck.getAction().toString()));
		}
	}

	/**
	 * Requests the Profile Template from GeoAxis for the specified user
	 * 
	 * @param username
	 *            The username
	 * @return The profile template, as stored in GeoAxis, which dictates what the user can or cannot do within Piazza
	 */
	private ProfileTemplate getProfileTemplateForUser(String username) {
		// TODO: Connect to GeoAxis
		return new ProfileTemplate();
	}
}
