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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.idam.authn.PiazzaAuthenticator;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.authz.endpoint.EndpointAuthorizer;
import org.venice.piazza.idam.authz.throttle.ThrottleAuthorizer;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.authz.AuthorizationCheck;
import org.venice.piazza.idam.model.authz.AuthorizationException;

import model.logger.AuditElement;
import model.logger.Severity;
import model.response.AuthResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.UUIDResponse;
import model.security.authz.UserProfile;
import util.PiazzaLogger;
import util.UUIDFactory;

/**
 * Controller that handles the User and Role requests for security information.
 * 
 * @author Russell.Orf
 */
@RestController
public class AuthController {
	@Autowired
	private PiazzaLogger pzLogger;
	@Autowired
	private MongoAccessor mongoAccessor;
	@Autowired
	private PiazzaAuthenticator piazzaAuthenticator;
	@Autowired
	private UUIDFactory uuidFactory;
	@Autowired
	private HttpServletRequest request;
	@Autowired
	private ThrottleAuthorizer throttleAuthorizer;
	@Autowired
	private EndpointAuthorizer endpointAuthorizer;

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
	private static final String IDAM_COMPONENT_NAME = "IDAM";
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
	 * Verifies that an API key is valid. Authentication.
	 * 
	 * @param body
	 *            A JSON object containing the 'uuid' field.
	 * 
	 * @return AuthResponse object containing the verification boolean of true or false
	 */
	@RequestMapping(value = "/authn", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AuthResponse> authenticateApiKey(@RequestBody Map<String, String> body) {
		try {
			String uuid = body.get("uuid");
			if (uuid != null) {
				if (mongoAccessor.isApiKeyValid(uuid)) {
					// Look up the user profile
					UserProfile userProfile = mongoAccessor.getUserProfileByApiKey(uuid);
					pzLogger.log("Verified API Key.", Severity.INFORMATIONAL,
							new AuditElement(userProfile.getUsername(), "verifiedApiKey", ""));
					// Send back the success
					return new ResponseEntity<>(new AuthResponse(true, userProfile), HttpStatus.OK);
				} else {
					// Record the error
					pzLogger.log("Unable to verify API Key.", Severity.INFORMATIONAL,
							new AuditElement("idam", "failedToVerifyApiKey", uuid));
					return new ResponseEntity<>(new AuthResponse(false), HttpStatus.UNAUTHORIZED);
				}

			} else {
				pzLogger.log("Received a null API Key during verification.", Severity.INFORMATIONAL);
				return new ResponseEntity<>(new AuthResponse(false, "API Key is null."), HttpStatus.BAD_REQUEST);
			}
		} catch (Exception exception) {
			String error = String.format("Error authenticating UUID: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			return new ResponseEntity<>(new AuthResponse(false, error), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Authorization check. Parameters define the username requesting an action.
	 * 
	 * @param authCheck
	 *            The model holding the username and the action
	 * @return Auth response. This contains the Boolean which determines if the user is able to perform the specified
	 *         action, and additional information for details of the check.
	 */
	@RequestMapping(value = "/authz", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AuthResponse> authenticateAndAuthorize(@RequestBody AuthorizationCheck authorizationCheck) {
		try {
			pzLogger.log("Checking Authorization for Action.", Severity.INFORMATIONAL,
					new AuditElement(authorizationCheck.getUsername(), "authorizationCheckForAction", authorizationCheck.toString()));

			// If the API Key is specified, then also perform authentication before doing the authorization check.
			// Check the validity of the API Key
			if (authorizationCheck.getApiKey() != null) {
				if (!mongoAccessor.isApiKeyValid(authorizationCheck.getApiKey())) {
					throw new AuthorizationException("Failed to Authenticate.", new AuthResponse(false, "Invalid API Key."));
				}
				// Ensure the API Key matches the Payload
				if (!mongoAccessor.getUsername(authorizationCheck.getApiKey()).equals(authorizationCheck.getUsername())) {
					throw new AuthorizationException("Failed to Authenticate.",
							new AuthResponse(false, "API Key identity does not match the authorization check username."));
				}
			}

			// Loop through all Authorizations and check if the action is permitted by each
			for (Authorizer authorizer : authorizers) {
				AuthResponse response = authorizer.canUserPerformAction(authorizationCheck);
				if (response.getIsAuthSuccess().booleanValue() == false) {
					pzLogger.log("Failed authorization check.", Severity.INFORMATIONAL,
							new AuditElement(authorizationCheck.getUsername(), "authorizationCheckFailed", authorizationCheck.toString()));
					throw new AuthorizationException("Failed to Authorize", response);
				}
			}

			// Return successful response.
			pzLogger.log("Passed authorization check.", Severity.INFORMATIONAL,
					new AuditElement(authorizationCheck.getUsername(), "authorizationCheckPassed", authorizationCheck.toString()));
			return new ResponseEntity<AuthResponse>(new AuthResponse(true), HttpStatus.OK);
		} catch (AuthorizationException authException) {
			String error = String.format("%s: %s", authException.getMessage(), authException.getResponse().getDetails().toString());
			LOGGER.error(error, authException);
			pzLogger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<AuthResponse>(new AuthResponse(false, error), HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			// Logging
			String error = String.format("Error checking authorization: %s: %s", authorizationCheck.toString(), exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<AuthResponse>(new AuthResponse(false, error), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retrieves a UUID based on the provided username and credential
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential' fields.
	 * 
	 * @return String UUID generated from the UUIDFactory in pz-jobcommon
	 */
	@RequestMapping(value = "/key", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> retrieveUUID() {
		try {
			String headerValue = request.getHeader("Authorization");
			String username = null;
			String uuid = null;

			if (headerValue != null) {
				String[] headerParts = headerValue.split(" ");

				if (headerParts.length == 2) {

					String decodedAuthNInfo = new String(Base64.getDecoder().decode(headerParts[1]), StandardCharsets.UTF_8);

					// PKI Auth
					if (decodedAuthNInfo.split(":").length == 1) {
						AuthResponse authResponse = piazzaAuthenticator.getAuthenticationDecision(decodedAuthNInfo.split(":")[0]);
						if (authResponse.getIsAuthSuccess()) {
							username = authResponse.getUserProfile().getUsername();
							uuid = uuidFactory.getUUID();
						}
					}

					// BASIC Auth
					else if (decodedAuthNInfo.split(":").length == 2) {
						String[] decodedUserPassParts = decodedAuthNInfo.split(":");
						username = decodedUserPassParts[0];
						String credential = decodedUserPassParts[1];

						if (piazzaAuthenticator.getAuthenticationDecision(username, credential).getIsAuthSuccess()) {
							uuid = uuidFactory.getUUID();
						}
					}

					if (uuid != null && username != null) {
						if (mongoAccessor.getApiKey(username) != null) {
							mongoAccessor.updateApiKey(username, uuid);
						} else {
							mongoAccessor.createApiKey(username, uuid);
						}

						pzLogger.log("Successfully verified Key.", Severity.INFORMATIONAL,
								new AuditElement(username, "generateApiKey", uuid));
						return new ResponseEntity<>(new UUIDResponse(uuid), HttpStatus.OK);
					}
				}
			}

			String error = "Authentication failed for user " + username;
			pzLogger.log(error, Severity.INFORMATIONAL, new AuditElement(username, "failedToGenerateKey", ""));
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			String error = String.format("Error retrieving UUID: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}