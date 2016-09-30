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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.idam.data.FileAccessor;
import org.venice.piazza.idam.data.Stats;

/**
 * Controller that handles the Admin requests
 * 
 * @author Russell.Orf
 */
@RestController
public class AdminController {

	@Autowired
	private FileAccessor fa;

	private final static Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
	
	/**
	 * Healthcheck required for all Piazza Core Services
	 * 
	 * @return String
	 */
	@RequestMapping(value = "/", method = RequestMethod.GET)
	public String getHealthCheck() {
		return "Hello, Health Check here.";
	}

	/**
	 * Retrieves a Stats object with statistics for the Piazza users and roles
	 * 
	 * @return Stats object containing the relevant user and role statistics
	 */
	@RequestMapping(value = "/admin/stats", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Stats getStats() {
		try {
			return fa.getStats();
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}
}