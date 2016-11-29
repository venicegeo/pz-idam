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
package org.venice.piazza.idam.controller;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.authz.endpoint.EndpointAuthorizer;
import org.venice.piazza.idam.authz.throttle.ThrottleAuthorizer;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.AuthResponse;
import org.venice.piazza.idam.model.authz.AuthorizationCheck;
import org.venice.piazza.idam.model.authz.AuthorizationException;

import model.logger.AuditElement;
import model.logger.Severity;
import util.PiazzaLogger;

/**
 * Controller that provides a consolidated endpoint for Authorization calls.
 * 
 * @author Patrick.Doody
 *
 */
@RestController
public class AuthorizationController {
	@Autowired
	private PiazzaLogger logger;
	@Autowired
	private ThrottleAuthorizer throttleAuthorizer;
	@Autowired
	private EndpointAuthorizer endpointAuthorizer;
	@Autowired
	private MongoAccessor mongoAccessor;

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthorizationController.class);
	private List<Authorizer> authorizers = new ArrayList<Authorizer>();

	/**
	 * Collects all of the Authorizers into a list that can be iterated through for a specific authorization check.
	 */
	@PostConstruct
	private void initializeAuthorizers() {
		authorizers.add(endpointAuthorizer);
		authorizers.add(throttleAuthorizer);
	}

	/**
	 * Authorization check. Parameters define the username requesting an action.
	 * 
	 * @param authCheck
	 *            The model holding the username and the action
	 * @return Authorization response. This contains the Boolean which determines if the user is able to perform the
	 *         specified action, and additional information for details of the check.
	 */
	@RequestMapping(value = "/authorization", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AuthResponse> canUserPerformAction(@RequestBody AuthorizationCheck authorizationCheck) {
		try {
			logger.log("Checking Authorization for Action.", Severity.INFORMATIONAL,
					new AuditElement(authorizationCheck.getUsername(), "authorizationCheckForAction", authorizationCheck.toString()));

			// Loop through all Authorizations and check if the action is permitted by each
			for (Authorizer authorizer : authorizers) {
				AuthResponse response = authorizer.canUserPerformAction(authorizationCheck);
				if (response.getAuthorized().booleanValue() == false) {
					logger.log("Failed authorization check.", Severity.INFORMATIONAL,
							new AuditElement(authorizationCheck.getUsername(), "authorizationCheckFailed", authorizationCheck.toString()));
					throw new AuthorizationException("Failed to Authorize", response);
				}
			}

			// Return successful response.
			logger.log("Passed authorization check.", Severity.INFORMATIONAL,
					new AuditElement(authorizationCheck.getUsername(), "authorizationCheckPassed", authorizationCheck.toString()));
			return new ResponseEntity<AuthResponse>(new AuthResponse(true), HttpStatus.OK);
		} catch (AuthorizationException authException) {
			String error = String.format("%s: %s", authException.getMessage(), authException.getResponse().getDetails().toString());
			LOGGER.error(error, authException);
			logger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<AuthResponse>(new AuthResponse(false, error), HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			// Logging
			String error = String.format("Error checking authorization: %s: %s", authorizationCheck.toString(), exception.getMessage());
			LOGGER.error(error, exception);
			logger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<AuthResponse>(new AuthResponse(false, error), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}
