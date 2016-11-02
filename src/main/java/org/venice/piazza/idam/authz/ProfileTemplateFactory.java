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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import model.security.authz.ProfileTemplate;
import model.security.authz.Throttle;

/**
 * Factory class which is capable of creating ProfileTemplate Models for various default groups. Used for initial
 * population of users within Piazza.
 * 
 * @author Patrick.Doody
 *
 */
public class ProfileTemplateFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProfileTemplateFactory.class);
	private ObjectMapper mapper = new ObjectMapper();

	/**
	 * Gets the ProfileTemplate for the specified role. This role must have an accompanying .json file in the
	 * resources/permissions and resources/throttles directory, which can be read from.
	 * <p>
	 * The ID is not set.
	 * </p>
	 * 
	 * @param role
	 *            The role
	 * @return ProfileTemplate for the specified role.
	 */
	public ProfileTemplate getTemplate(String role) throws IOException {
		ProfileTemplate profileTemplate = new ProfileTemplate();
		// Get the Permissions from the local .json file
		profileTemplate.setPermissions(getPermissions(role));
		// Populate the default Throttle values
		profileTemplate.setThrottles(getThrottles(role));
		// Empty container for third party keys
		profileTemplate.setThirdPartyKeys(new HashMap<String, String>());

		return profileTemplate;
	}

	/**
	 * Gets the list of default Throttle values for the specified role. Reads from the .json file in the throttles
	 * directory.
	 * 
	 * @param role
	 *            The role
	 * @return The list of permissions
	 */
	private List<Throttle> getThrottles(String role) {
		// TODO
		return null;
	}

	/**
	 * Gets the Permissions Map for the specified role. Reads from the .json file in the permissions directory.
	 * 
	 * @param role
	 *            The role
	 * @return The Permissions map
	 */
	private Map<String, Boolean> getPermissions(String role) throws IOException {
		// Load the .json resource
		ClassLoader classLoader = getClass().getClassLoader();
		InputStream templateStream = null;
		String templateString = null;
		try {
			templateStream = classLoader.getResourceAsStream(String.format("%s%s%s", "permissions", File.separator, role));
			templateString = IOUtils.toString(templateStream);
		} finally {
			try {
				templateStream.close();
			} catch (Exception exception) {
				LOGGER.error("Error closing GeoServer Template Stream.", exception);
			}
		}
		// Read the String into the permissions map
		TypeReference<HashMap<String, Boolean>> typeRef = new TypeReference<HashMap<String, Boolean>>() {
		};
		Map<String, Boolean> permissions = mapper.readValue(templateString, typeRef);
		return permissions;
	}
}
