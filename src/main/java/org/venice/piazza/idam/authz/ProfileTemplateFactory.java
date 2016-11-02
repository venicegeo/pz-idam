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
package org.venice.piazza.idam.authz;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import model.security.authz.ProfileTemplate;

/**
 * Factory class which is capable of creating ProfileTemplate Models for various default groups. Used for initial
 * population of users within Piazza.
 * 
 * @author Patrick.Doody
 *
 */
@Component
public class ProfileTemplateFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileTemplateFactory.class);
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * Gets the ProfileTemplate for the specified role. This role must have an accompanying .json file in the
	 * resources/profiles folder.
	 * <p>
	 * The ID is not set.
	 * </p>
	 * 
	 * @param role
	 *            The role
	 * @return ProfileTemplate for the specified role.
	 */
	public ProfileTemplate getDefaultTemplate(String role) throws IOException {
		// Load the .json resource
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream templateStream = null;
		String templateString = null;
		try {
			templateStream = classLoader.getResourceAsStream(String.format("%s%s%s", "profiles", File.separator, role));
			templateString = IOUtils.toString(templateStream);
		} finally {
			try {
				templateStream.close();
			} catch (Exception exception) {
				LOGGER.error("Error closing GeoServer Template Stream.", exception);
			}
		}
		// Read the String into the Template object
		return mapper.readValue(templateString, ProfileTemplate.class);
	}

	/**
	 * Requests the Profile Template from GeoAxis for the specified user
	 * 
	 * @param username
	 *            The username
	 * @return The profile template, as stored in GeoAxis, which dictates what the user can or cannot do within Piazza
	 */
	public ProfileTemplate getProfileTemplateForUser(String username) throws IOException {
		// TODO: Connect to GeoAxis
		return getDefaultTemplate("admin");
	}
}
