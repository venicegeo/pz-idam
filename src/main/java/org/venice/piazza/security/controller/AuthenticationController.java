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
package org.venice.piazza.security.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.security.data.LDAPAccessor;
import org.venice.piazza.security.data.MongoAccessor;

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
	private LDAPAccessor ldapAccessor;
	@Autowired
	private UUIDFactory uuidFactory;

	/**
	 * Retrieves an authentication decision based on the provided username and
	 * credential
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential'
	 *            fields.
	 * 
	 * @return boolean flag indicating true if verified, false if not.
	 */
	@RequestMapping(value = "/verification", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public boolean authenticateUserByUserPass(@RequestBody Map<String, String> body) {
		try {
			return ldapAccessor.getAuthenticationDecision(body.get("username"), body.get("credential"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Retrieves an authentication decision based on the provided username and
	 * credential
	 * 
	 * @param body
	 *            A JSON object containing the 'username' and 'credential'
	 *            fields.
	 * 
	 * @return boolean flag indicating true if verified, false if not.
	 */
	@RequestMapping(value = "/v2/verification", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> authenticateUserByUUID(@RequestBody Map<String, String> body) {
		try {
			String uuid = body.get("uuid");
			if (uuid != null) {
				return new ResponseEntity<PiazzaResponse>(new AuthenticationResponse(mongoAccessor.getUsername(uuid),
						mongoAccessor.getAuthenticationDecision(uuid)), HttpStatus.OK);
			} else {
				return new ResponseEntity<PiazzaResponse>(new ErrorResponse("UUID is null!", "Security"),
						HttpStatus.INTERNAL_SERVER_ERROR);
			}
		} catch (Exception exception) {
			exception.printStackTrace();
			String error = String.format("Error authenticating UUID: %s", exception.getMessage());
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
	@RequestMapping(value = "/key", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> retrieveUUID(@RequestBody Map<String, String> body) {
		try {
			String username = body.get("username");
			String uuid = null;

			if (authenticateUserByUserPass(body)) {
				if ((uuid = mongoAccessor.getUuid(username)) == null) {
					uuid = uuidFactory.getUUID();
					mongoAccessor.save(username, uuid);
				}

				return new ResponseEntity<PiazzaResponse>(new UUIDResponse(uuid), HttpStatus.OK);
			}

			return new ResponseEntity<PiazzaResponse>(
					new ErrorResponse("Authentication failed for user " + username, "Security"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (Exception exception) {
			exception.printStackTrace();
			String error = String.format("Error retrieving UUID: %s", exception.getMessage());
			logger.log(error, PiazzaLogger.ERROR);
			return new ResponseEntity<PiazzaResponse>(new ErrorResponse(error, "Security"),
					HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}