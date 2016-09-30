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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.venice.piazza.idam.data.FileAccessor;

/**
 * Controller that handles the User and Role requests for security information.
 * 
 * @author Russell.Orf
 */
@RestController
public class RoleManagementController {

	@Autowired
	private FileAccessor fa;

	private static final String SUCCESSES = "Successes";
	private static final String FAILURES = "Failures";
	private static final String STATUS = "Status";

	private final static Logger LOGGER = LoggerFactory.getLogger(RoleManagementController.class);
	
	/**
	 * Retrieves all of the users defined in the system.
	 * 
	 * @return Set<String> object
	 */
	@RequestMapping(value = "/users", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Set<String> getUsers() {
		try {
			return fa.getUsers();
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}

	/**
	 * Retrieves all of the roles for a given user.
	 * 
	 * @param userid
	 *            The username of the user to retrieve the defined roles for.
	 * 
	 * @return List<String> object in the form "username":"role1,role2,role3"
	 */
	@RequestMapping(value = "/users/{userid}/roles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public List<String> getRoles(@PathVariable(value = "userid") String userid) {
		try {
			return fa.getRolesForUser(userid);
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}

	/**
	 * Retrieves all of the roles for every user.
	 * 
	 * @return Map<String,String> object in the form
	 *         "username":"role1,role2,role3"
	 */
	@RequestMapping(value = "/users/roles", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> getUsersAndRoles() {
		try {
			return fa.getUsersAndRoles();
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}

	/**
	 * Deletes a user based on the provided username
	 * 
	 * @param userid
	 *            The username of the user to delete
	 * 
	 * @return Map<String,String> object in the form "status":"The status"
	 */
	@RequestMapping(value = "/users/{userid}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> deleteUser(@PathVariable(value = "userid") String userid) {
		Map<String, String> response = new HashMap<String, String>();
		try {
			userid = userid.toLowerCase();
			if (!fa.userExists(userid)) {
				response.put(STATUS, "User '" + userid + "' does not exist!");
			} else {
				fa.removeUser(userid);
				response.put(STATUS, "User '" + userid + "' deleted.");
			}
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			response.put(STATUS, "Exception: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Deletes a single role from the provided username
	 * 
	 * @param userid
	 *            The username of the user to delete the role from.
	 * 
	 * @param role
	 *            The role to be deleted from the user.
	 * 
	 * @return Map<String,String> object in the form "status":"The status"
	 */
	@RequestMapping(value = "/users/{userid}/roles/{role}", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> deleteRoleFromUser(@PathVariable(value = "userid") String userid,
			@PathVariable(value = "role") String role) {
		Map<String, String> response = new HashMap<String, String>();
		try {
			userid = userid.toLowerCase();
			role = role.toLowerCase();
			if (!fa.userExists(userid)) {
				response.put(STATUS, "User '" + userid + "' does not exist!");
			} else if (!fa.roleExists(userid, role)) {
				response.put(STATUS, "Role '" + role + "' does not exist for user '" + userid + "'");
			} else {
				fa.removeRole(userid, role);
				response.put(STATUS, "Role '" + role + "' deleted for user '" + userid + "'");
			}
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			response.put(STATUS, "Exception: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Deletes all roles from the provided username
	 * 
	 * @param userid
	 *            The username of the user to delete the roles from.
	 * 
	 * @return Map<String,String> object in the form "status":"The status"
	 */
	@RequestMapping(value = "/users/{userid}/roles", method = RequestMethod.DELETE, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> deleteAllRolesFromUser(@PathVariable(value = "userid") String userid) {
		Map<String, String> response = new HashMap<String, String>();
		try {
			userid = userid.toLowerCase();
			if (!fa.userExists(userid)) {
				response.put(STATUS, "User '" + userid + "' does not exist!");
			} else {
				fa.removeAllRoles(userid);
				response.put(STATUS, "All roles for user '" + userid + "' deleted.");
			}
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			response.put(STATUS, "Exception: " + e.getMessage());
		}
		return response;
	}

	/**
	 * Adds users and their respective roles to the system based on the provided
	 * usernames
	 * 
	 * @param body
	 *            A list of users and their respective roles to add
	 * 
	 * @return Map<String,String> object in the form "status":"The status"
	 */
	@RequestMapping(value = "/users/roles", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, List<String>> addUsersAndRoles(@RequestBody Map<String, String> body) {
		try {
			List<String> successes = new ArrayList<String>();
			List<String> failures = new ArrayList<String>();
			Map<String, String> usersToAdd = new HashMap<String, String>();

			for (Map.Entry<String, String> entry : body.entrySet()) {
				String userid = entry.getKey().toLowerCase();
				if (!fa.userExists(userid)) {
					usersToAdd.put(userid, userid + ":" + entry.getValue());
					successes.add("User '" + userid + "' inserted with roles: " + entry.getValue().toLowerCase());
				} else {
					failures.add("User '" + userid + "' already exists!");
				}
			}
			fa.addUsers(usersToAdd);

			Map<String, List<String>> response = new HashMap<String, List<String>>();
			response.put(SUCCESSES, successes);
			response.put(FAILURES, failures);
			return response;
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}

	/**
	 * Adds users to the system based on the provided usernames
	 * 
	 * @param users
	 *            A list of users to add
	 * 
	 * @return Map<String,String> object in the form "status":"The status"
	 */
	@RequestMapping(value = "/users", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, List<String>> addUsers(@RequestBody List<String> users) {
		try {
			List<String> successes = new ArrayList<String>();
			List<String> failures = new ArrayList<String>();
			Map<String, String> usersToAdd = new HashMap<String, String>();

			for (String userid : users) {
				userid = userid.toLowerCase();
				if (!fa.userExists(userid)) {
					usersToAdd.put(userid, userid + ":");
					successes.add("User '" + userid + "' inserted with no roles.");
				} else {
					failures.add("User '" + userid + "' already exists!");
				}
			}
			fa.addUsers(usersToAdd);

			Map<String, List<String>> response = new HashMap<String, List<String>>();
			response.put(SUCCESSES, successes);
			response.put(FAILURES, failures);
			return response;
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			return null;
		}
	}

	/**
	 * Replaces a users list of roles based with the roles provided, erasing any
	 * previously defined roles.
	 * 
	 * @param userid
	 *            The username of the user to update the roles for.
	 * 
	 * @param roles
	 *            The list of roles to assign to the user. This erases the
	 *            previously defined roles.
	 * 
	 * @return Map<String,String> object in the form "status":"The status"
	 */
	@RequestMapping(value = "/users/{userid}/roles", method = RequestMethod.PUT, produces = MediaType.APPLICATION_JSON_VALUE)
	public Map<String, String> updateRolesForUser(@PathVariable(value = "userid") String userid,
			@RequestBody List<String> roles) {

		Map<String, String> response = new HashMap<String, String>();
		try {
			userid = userid.toLowerCase();
			if (fa.userExists(userid)) {
				fa.updateUserRoles(userid, roles);
				response.put(STATUS, "User '" + userid + "' updated with roles: " + fa.getRolesForUser(userid));
			} else {
				response.put(STATUS, "User '" + userid + "' does not exist!");
			}
		} catch (Exception e) {
			LOGGER.error(Arrays.toString(e.getStackTrace()));
			response.put(STATUS, "Exception: " + e.getMessage());
		}
		return response;
	}
}