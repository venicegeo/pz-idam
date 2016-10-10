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
import java.util.Arrays;
import java.util.Base64;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

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
import org.venice.piazza.idam.authn.PiazzaAuthenticator;
import org.venice.piazza.idam.data.MongoAccessor;

import model.response.AuthenticationResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.UUIDResponse;
import util.PiazzaLogger;
import util.UUIDFactory;

/**
 * Controller that handles the User and Role requests for security information.
 * 
 * @author Russell.Orf
 */
@RestController
public class AuthenticationController {
	@Autowired
	private PiazzaLogger logger;
	@Autowired
	private MongoAccessor mongoAccessor;
	@Autowired
	private PiazzaAuthenticator piazzaAuthenticator;
	@Autowired
	private UUIDFactory uuidFactory;
	@Autowired
	private HttpServletRequest request;

	private final static Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);
	
	/**
	 * Verifies that an API key is valid.
	 * 
	 * @param body
	 *            A JSON object containing the 'uuid' field.
	 * 
	 * @return AuthenticationResponse object containing the verification boolean of true or false
	 */
	@RequestMapping(value = "/v2/verification", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> authenticateUserByUUID(@RequestBody Map<String, String> body) {
		try {
			String uuid = body.get("uuid");
			if (uuid != null) {
				return new ResponseEntity<PiazzaResponse>(
						new AuthenticationResponse(mongoAccessor.getUsername(uuid), mongoAccessor.isAPIKeyValid(uuid)), HttpStatus.OK);
			} else {
				return new ResponseEntity<PiazzaResponse>(new ErrorResponse("UUID is null!", "Security"),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception exception) {
			String error = String.format("Error authenticating UUID: %s", exception.getMessage());
			LOGGER.error(error, exception);
			logger.log(error, PiazzaLogger.ERROR);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Security"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Retrieves a UUID based on the provided username and credential
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential'
	 *            fields.
	 * 
	 * @return String UUID generated from the UUIDFactory in pz-jobcommon
	 */
	@RequestMapping(value = "/key", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> retrieveUUID() {
		try {
			String headerValue = request.getHeader("Authorization");
			String username = null;
			String credential = null;
			String uuid = null;
			String decodedAuthNInfo = null;
			String[] headerParts, decodedUserPassParts;

			if (headerValue != null) {
				headerParts = headerValue.split(" ");
				
				if (headerParts.length == 2 ) {
					
					decodedAuthNInfo = new String(Base64.getDecoder().decode(headerParts[1]), StandardCharsets.UTF_8);
					
					// PKI Auth
					if( decodedAuthNInfo.split(":").length == 1) {
						AuthenticationResponse authResponse = piazzaAuthenticator.getAuthenticationDecision(decodedAuthNInfo.split(":")[0]);
						if( authResponse.getAuthenticated() ) {
							username = authResponse.getUsername(); 
							uuid = uuidFactory.getUUID();
						}						
					}
					
					// BASIC Auth
					else if ( decodedAuthNInfo.split(":").length == 2) {
						decodedUserPassParts = decodedAuthNInfo.split(":");
						username = decodedUserPassParts[0];
						credential = decodedUserPassParts[1];
						
						if (piazzaAuthenticator.getAuthenticationDecision(username, credential).getAuthenticated()) {
							uuid = uuidFactory.getUUID();
						}
					}
					
					if( uuid != null && username != null ) {
						if (mongoAccessor.getUuid(username) != null) {
							mongoAccessor.update(username, uuid);
						} else {
							mongoAccessor.save(username, uuid);
						}

						return new ResponseEntity<PiazzaResponse>(new UUIDResponse(uuid), HttpStatus.OK);
					}
				}
			}

			return new ResponseEntity<PiazzaResponse>(
					new ErrorResponse("Authentication failed for user " + username, "Security"),
					HttpStatus.UNAUTHORIZED);
		} catch (Exception exception) {
			String error = String.format("Error retrieving UUID: %s", exception.getMessage());
			LOGGER.error(error, exception);			
			logger.log(error, PiazzaLogger.ERROR);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Security"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}