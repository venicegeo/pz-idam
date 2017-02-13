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

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.user.UserThrottles;

import model.logger.AuditElement;
import model.logger.Severity;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.UserProfileResponse;
import model.security.authz.UserProfile;
import util.PiazzaLogger;

/**
 * Controller that handles the Admin requests
 * 
 * @author Russell.Orf, Patrick.Doody
 */
@RestController
public class AdminController {
	@Autowired
	private PiazzaLogger pzLogger;
	@Autowired
	private Environment env;
	@Autowired
	private MongoAccessor mongoAccessor;

	private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);

	/**
	 * Healthcheck required for all Piazza Core Services
	 * 
	 * @return String
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getHealthCheck() {
		return "Hello, Health Check here for pz-idam.";
	}

	@RequestMapping(value = "/admin/stats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public String getAdminStats() {
		return "{ \"profiles\":\"" + String.join(",", env.getActiveProfiles()) + "\" }";
	}

	/**
	 * Returns all User Throttles
	 * 
	 * @return User Throttles
	 */
	@RequestMapping(value = "/admin/throttles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<UserThrottles> getAllUserThrottles() {
		return mongoAccessor.getAllUserThrottles();
	}

	/**
	 * Returns the User Profile information by username
	 * 
	 * @param username
	 *            The username to fetch the profile for
	 * @return The User Profile information
	 */
	@RequestMapping(value = "/profile/{username}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PiazzaResponse> getUserProfile(@PathVariable(value = "username") String username) {
		try {
			// Validate
			if ((username == null) || (username.isEmpty())) {
				// Bad Input
				String error = "Cannot retrieve Profile: `username` parameter not specified.";
				LOGGER.info(error);
				pzLogger.log(error, Severity.INFORMATIONAL);
				return new ResponseEntity<>(new ErrorResponse(error, "IDAM"), HttpStatus.BAD_REQUEST);
			}

			// Check for Profile
			UserProfile userProfile = mongoAccessor.getUserProfileByUsername(username);
			if (userProfile != null) {
				// Audit the Retrieval
				pzLogger.log(String.format("Retrieved Profile for user %s.", username), Severity.INFORMATIONAL,
						new AuditElement(username, "userProfileCheckSuccess", ""));
				// Return the Profile
				return new ResponseEntity<>(new UserProfileResponse(userProfile), HttpStatus.OK);
			} else {
				// No Profile Found
				String error = "No User Profile found from specified Username " + username;
				LOGGER.info(error);
				pzLogger.log(error, Severity.INFORMATIONAL);
				return new ResponseEntity<>(new ErrorResponse(error, "IDAM"), HttpStatus.NOT_FOUND);
			}
		} catch (Exception exception) {
			String error = String.format("Error Getting User Profile: %s", exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
			return new ResponseEntity<>(new ErrorResponse(error, "IDAM"), HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}
}