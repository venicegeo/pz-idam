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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.user.UserThrottles;

/**
 * Controller that handles the Admin requests
 * 
 * @author Russell.Orf
 */
@RestController
public class AdminController {
	@Autowired
	private Environment env;
	@Autowired
	private MongoAccessor mongoAccessor;

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
}