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
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import org.venice.piazza.idam.authn.PiazzaAuthenticator;
import org.venice.piazza.idam.authz.Authorizer;
import org.venice.piazza.idam.authz.endpoint.EndpointAuthorizer;
import org.venice.piazza.idam.authz.throttle.ThrottleAuthorizer;
import org.venice.piazza.idam.data.DatabaseAccessor;
import org.venice.piazza.idam.model.GxOAuthResponse;
import org.venice.piazza.idam.model.authz.AuthorizationException;
import org.venice.piazza.idam.util.GxOAuthClient;

import model.logger.AuditElement;
import model.logger.Severity;
import model.response.AuthResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.SuccessResponse;
import model.response.UUIDResponse;
import model.security.authz.AuthorizationCheck;
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
	private DatabaseAccessor accessor;
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
	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private GxOAuthClient oAuthClient;

	private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);
	private static final String IDAM_COMPONENT_NAME = "IDAM";
	private List<Authorizer> authorizers = new ArrayList<Authorizer>();

	/**
	 * Collects all of the Authorizers into a list that can be iterated through for a specific authorization check.
	 */
	@PostConstruct
	public void initializeAuthorizers() {
		authorizers.clear();
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
				if (accessor.isApiKeyValid(uuid)) {
					// Look up the user profile
					UserProfile userProfile = accessor.getUserProfileByApiKey(uuid);
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
			// Log the check
			pzLogger.log("Checking Authorization for Action.", Severity.INFORMATIONAL,
					new AuditElement(authorizationCheck.getUsername(), "authorizationCheckForAction", authorizationCheck.toString()));

			// If the API Key is specified, then also perform authentication before doing the authorization check.
			// Check the validity of the API Key
			if (authorizationCheck.getApiKey() != null) {

				if (!accessor.isApiKeyValid(authorizationCheck.getApiKey())) {
					throw new AuthorizationException("Failed to Authenticate.", new AuthResponse(false, "Invalid API Key."));
				} 
				
				// If the API Key was specified, but the user name was not, then populate the username so that the
				// Authorizers below can function.				
				else if (authorizationCheck.getUsername() == null) {
					authorizationCheck.setUsername(accessor.getUsername(authorizationCheck.getApiKey()));
				}
				
				// Ensure the API Key matches the Payload
				if (!accessor.getUsername(authorizationCheck.getApiKey()).equals(authorizationCheck.getUsername())) {
					throw new AuthorizationException("Failed to Authenticate.", 
						new AuthResponse(false, "API Key identity does not match the authorization check username."));
				}
			}
			// If the user specifies neither an API Key or a Username, then the request parameters are insufficient.
			else if ((authorizationCheck.getUsername() == null) || (authorizationCheck.getUsername().isEmpty())) {
				throw new AuthorizationException("Incomplete request details", new AuthResponse(false, "API Key or Username not specified."));
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
			return new ResponseEntity<AuthResponse>(
					new AuthResponse(true, accessor.getUserProfileByUsername(authorizationCheck.getUsername())), HttpStatus.OK);
		} catch (AuthorizationException authException) {
			String error = String.format("%s: %s", authException.getMessage(), authException.getResponse().getDetails().toString());
			LOGGER.error(error, authException);
			pzLogger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<AuthResponse>(new AuthResponse(false, error), HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			// Logging
			String error = String.format("Error checking authorization: %s", authorizationCheck);
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<AuthResponse>(new AuthResponse(false, error), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Generates a new API Key based on the provided username and credential for GeoAxis.
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential' fields.
	 * 
	 * @return String UUID generated from the UUIDFactory in pz-jobcommon
	 */
	@RequestMapping(value = "/key", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> generateApiKey() {
		try {
			final String headerValue = request.getHeader("Authorization");
			String username = null;

			if (headerValue != null && headerValue.split(" ").length == 2) {
				final String[] headerParts = headerValue.split(" ");

				String decodedAuthNInfo = new String(Base64.getDecoder().decode(headerParts[1]), StandardCharsets.UTF_8);
				username = getAuthenticatedUsername(decodedAuthNInfo);

				if (username != null) {
					String uuid = uuidFactory.getUUID();
					updateAPIKey(username, uuid);

					// Return the Key
					pzLogger.log("Successfully verified Key.", Severity.INFORMATIONAL,
							new AuditElement(username, "generateApiKey", ""));
					return new ResponseEntity<>(new UUIDResponse(uuid), HttpStatus.CREATED);
				}
			}

			final String error = "Authentication failed for user " + username;
			pzLogger.log(error, Severity.INFORMATIONAL, new AuditElement(username, "failedToGenerateKey", ""));
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			String error = String.format("Error retrieving API Key: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.UNAUTHORIZED);
		}
	}

	private void updateAPIKey(final String username, final String uuid) {
		// Update the API Key in the UUID Collection
		if (accessor.getApiKey(username) != null) {
			accessor.updateApiKey(username, uuid);
		} else {
			accessor.createApiKey(username, uuid);
		}
	}
	
	/**
	 * Deletes API Key with provided UUID
	 * 
	 * @param key
	 * @return PiazzaResponse
	 */
	@RequestMapping(value = "/v2/key/{key}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> deleteApiKey(@PathVariable(value = "key") String uuid) {
		try {
			//Delete API Key
			String username = accessor.getUsername(uuid);
			accessor.deleteApiKey(uuid);
			
			//Log the action
			String response = String.format("User: %s API Key was deleted", username);
			pzLogger.log(response, Severity.INFORMATIONAL, new AuditElement(username, "deleteApiKey", ""));
			LOGGER.info(response);
			return new ResponseEntity<>(new SuccessResponse(response, IDAM_COMPONENT_NAME), HttpStatus.OK);
		} catch (Exception exception) {
			String error = String.format("Error deleting API Key: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
	
	/**
	 * Generates a new API Key based on the provided username and credential for GeoAxis.
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential' fields.
	 * 
	 * @return String UUID generated from the UUIDFactory in pz-jobcommon
	 */
	@RequestMapping(value = "/v2/key", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> generateApiKeyV2() {
		return generateApiKey();
	}

	/**
	 * Gets an existing key for the user. Does not generate one if it does not exist.
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential' fields.
	 * 
	 * @return String UUID generated from the UUIDFactory in pz-jobcommon
	 */
	@RequestMapping(value = "/v2/key", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> getExistingApiKey() {
		try {
			// Decode credentials. We need to get the username of this account.
			final String authHeader = request.getHeader("Authorization");
			String username = null;

			// Ensure the Authorization Header is present
			if (authHeader != null && authHeader.split(" ").length == 2) {
				final String[] headerParts = authHeader.split(" ");

				String decodedAuthNInfo = new String(Base64.getDecoder().decode(headerParts[1]), StandardCharsets.UTF_8);
				username = getAuthenticatedUsername(decodedAuthNInfo);
				
				// Username found and authenticated. Get the API Key.
				if (username != null) {
					String apiKey = accessor.getApiKey(username);
					return getExistingAPIKeyResponse(apiKey, username);
				}
			}
			// If the username was not found and authenticated from the auth header, then no API Key can be returned.
			// Return an error.
			String error = "Could not get existing API Key.";
			pzLogger.log(error, Severity.INFORMATIONAL, new AuditElement(username, "failedToGetExistingKey", ""));
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			// Log
			String error = String.format("Error retrieving API Key: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			// Return Error
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.UNAUTHORIZED);
		}
	}
	
	private String getAuthenticatedUsername(final String decodedAuthNInfo) {
		String username = null;
		AuthResponse authResponse = null;
		
		// PKI Auth - authenticate
		if (decodedAuthNInfo.split(":").length == 1) {
			authResponse = piazzaAuthenticator.getAuthenticationDecision(decodedAuthNInfo.split(":")[0]);
		}
		
		// BASIC Auth - authenticate
		else if (decodedAuthNInfo.split(":").length == 2) {
			String[] decodedUserPassParts = decodedAuthNInfo.split(":");
			authResponse = piazzaAuthenticator.getAuthenticationDecision(decodedUserPassParts[0], decodedUserPassParts[1]);
		}
		
		if (authResponse != null && authResponse.getIsAuthSuccess()) {
			username = authResponse.getUserProfile().getUsername();
		}
		
		return username;
	}
	
	private ResponseEntity<PiazzaResponse> getExistingAPIKeyResponse(final String apiKey, final String username) {
		// Ensure the apiKey is not null
		if (apiKey != null) {
			// Key Found
			pzLogger.log(String.format("Successfully retrieved API Key for user %s.", username), Severity.INFORMATIONAL,
					new AuditElement(username, "getExistingApiKey", ""));
			return new ResponseEntity<>(new UUIDResponse(apiKey), HttpStatus.OK);
		} else {
			// There is an account found, but there is no api key associated with it.
			pzLogger.log(String.format("%s requested existing API Key but none was found.", username),
					Severity.INFORMATIONAL, new AuditElement(username,
							"failedGetExistingApiKey", ""));
			String error = String.format("No active API Key found for %s. Please request a new API Key.", username);
			return new ResponseEntity<>(new ErrorResponse(error, IDAM_COMPONENT_NAME), HttpStatus.BAD_REQUEST);
		}
	}

	@RequestMapping(value = "/login/geoaxis", method = RequestMethod.GET)
	public RedirectView oauthRedirect() {
	    final String redirectUri = oAuthClient.getRedirectUri(request);
	    LOGGER.debug(String.format("login redirectUri = %s", redirectUri));
		return new RedirectView(oAuthClient.getOAuthUrlForGx(redirectUri));
	}

	@RequestMapping(value = "/login", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public RedirectView oauthResponse(@RequestParam String code, HttpSession session, HttpServletResponse response) {
		try {
			try {
				LOGGER.debug("Requesting access token...");
                final String redirectUri = oAuthClient.getRedirectUri(request);
				final String accessToken = oAuthClient.getAccessToken(code, redirectUri);

				LOGGER.debug("Requesting user profile...");
				final ResponseEntity<GxOAuthResponse> profileResponse = oAuthClient.getGxUserProfile(accessToken);

				final String username = profileResponse.getBody().getUsername();
				final String dn = profileResponse.getBody().getDn();

				// If there's no profile create one and make sure they have an api key
                LOGGER.debug(String.format("Checking user profile for %s with dn=%s", username, dn));
                if (!accessor.hasUserProfile(username, dn)) {
                    LOGGER.debug(String.format("Creating user profile for %s", username));
					UserProfile profile = oAuthClient.getUserProfileFromGxProfile(profileResponse.getBody());
					accessor.insertUserProfile(profile);
					accessor.createApiKey(username, uuidFactory.getUUID());
				}

				final UserProfile user = accessor.getUserProfileByUsername(username);
				String apiKey = accessor.getApiKey(username);

				// If key is invalid, delete and reissue
				if (!accessor.isApiKeyValid(apiKey)) {
					accessor.deleteApiKey(apiKey);
					apiKey = uuidFactory.getUUID();
					accessor.createApiKey(username, apiKey);
				}

				//session.setAttribute("api_key", apiKey);
				// TODO: We probably don't need both of these. Remove the one we don't need.
				Cookie cookie = new Cookie("api_key", apiKey);
				cookie.setHttpOnly(true);
				cookie.setSecure(true);
				int dotIndex = request.getServerName().indexOf(".") + 1;
				String domain = request.getServerName().substring(dotIndex);
				cookie.setDomain(domain);
				response.addCookie(cookie);

				return new RedirectView(String.format("https://{}?logged_in=true", oAuthClient.getUiUrl(request)));
			} catch (HttpClientErrorException | HttpServerErrorException hee) {
				LOGGER.error(hee.getResponseBodyAsString(), hee);
				RedirectView errorView = new RedirectView();
				errorView.setStatusCode(hee.getStatusCode());
				return errorView;
			}
		} catch (Exception exception) {
			String error = String.format("Error retrieving API Key: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			RedirectView errorView = new RedirectView();
			errorView.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
			return errorView;
		}
	}

	@RequestMapping(value = "/logout", method = RequestMethod.GET)
	public RedirectView oauthLogout(HttpSession session)
	{
		// Clear the sesseion and forward to gx logout
        LOGGER.info("Logout user");
        session.invalidate();
		return new RedirectView(oAuthClient.getLogoutUrl());
	}


}