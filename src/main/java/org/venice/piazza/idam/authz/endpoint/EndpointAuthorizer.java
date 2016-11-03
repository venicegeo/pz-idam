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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.authz.ProfileTemplateFactory;
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
	private PiazzaLogger pzLogger;
	@Autowired
	private ProfileTemplateFactory profileTemplateFactory;

	private static final Logger LOGGER = LoggerFactory.getLogger(EndpointAuthorizer.class);

	@Override
	public AuthResponse canUserPerformAction(AuthorizationCheck authorizationCheck) {
		// Get the Profile Template for the user requesting the action. This Template contains endpoint information.
		String username = authorizationCheck.getUsername();
		ProfileTemplate profileTemplate;
		try {
			profileTemplate = profileTemplateFactory.getProfileTemplateForUser(username);
		} catch (IOException exception) {
			String message = String.format("Error getting Profile Template from provider for %s Cannot grant access as a result. Error: %s",
					authorizationCheck.toString(), exception.getMessage());
			LOGGER.error(message, exception);
			pzLogger.log(message, PiazzaLogger.ERROR);
			return new AuthResponse(false, message);
		}
		// Get the appropriate Permission from the Profile Template that matches the Permission
		String keyName = authorizationCheck.getAction().getKeyName();
		// Ensure the key exists
		if (profileTemplate.getPermissions().containsKey(keyName) == false) {
			pzLogger.log(String.format("%s denied by Endpoint Authorizer. Method and URI name does not contain a Permission.",
					authorizationCheck.toString()), PiazzaLogger.INFO);
			return new AuthResponse(false,
					String.format("The %s endpoint does not have a defined Permission.", authorizationCheck.getAction().toString()));
		}
		boolean canPerformAction = profileTemplate.getPermissions().get(keyName);
		if (canPerformAction) {
			pzLogger.log(String.format("%s granted by Endpoint Authorizer.", authorizationCheck.toString()), PiazzaLogger.INFO);
			return new AuthResponse(true);
		} else {
			pzLogger.log(String.format("%s denied by Endpoint Authorizer.", authorizationCheck.toString()), PiazzaLogger.INFO);
			return new AuthResponse(false, String.format("The user does not have the ability to access the %s endpoint.",
					authorizationCheck.getAction().toString()));
		}
	}
}
