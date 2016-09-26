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
package org.venice.piazza.idam.test.controller;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.controller.AdminController;
import org.venice.piazza.idam.controller.AuthenticationController;
import org.venice.piazza.idam.controller.RoleManagementController;
import org.venice.piazza.idam.data.FileAccessor;
import org.venice.piazza.idam.data.LDAPAccessor;
import org.venice.piazza.idam.data.Stats;

public class SecurityTests {

	@Mock
	private FileAccessor fa;

	@Mock
	private LDAPAccessor ldapClient;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private AdminController adminController;

	@InjectMocks
	private AuthenticationController authenticationController;
	
	@InjectMocks
	private RoleManagementController roleManagementController;	
	
	
	private Set<String> users = new HashSet<String>(Arrays.asList("dummy"));

	private List<String> roles = new ArrayList<String>(
			Arrays.asList("abort", "access", "admin-stats", "delete-service", "execute-service", "get", "get-resource",
					"ingest", "list-service", "read-service", "register-service", "search-service", "update-service"));

	private Map<String, String> usersAndRoles = new HashMap<String, String>();
	private Map<String, String> usersAndCredentialsAndRoles = new HashMap<String, String>();

	private Stats stats;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		// Mock a Service to use
		stats = new Stats(1, 2, 3, 4);

		usersAndRoles.put("dummy",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");

		usersAndCredentialsAndRoles.put("dummy",
				"dummy:abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
	}

	/**
	 * Tests root endpoint
	 */
	@Test
	public void testGetHealthCheck() {
		String result = adminController.getHealthCheck();
		assertTrue(result.contains("Hello"));
	}

	/**
	 * Test /admin/stats
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetStats() throws IOException {

		// Mock
		when(fa.getUsersAndRoles()).thenReturn(usersAndRoles);
		when(fa.getUsers()).thenCallRealMethod();
		when(fa.getNumRoles()).thenCallRealMethod();
		when(fa.getNumUsersWithNoRoles()).thenCallRealMethod();
		when(fa.getNumUsersWithAllRoles()).thenCallRealMethod();
		when(fa.getStats()).thenCallRealMethod();

		// Test
		Stats response = adminController.getStats();

		// Verify
		assertTrue(response.getNumRoles() >= 0);
		assertTrue(response.getNumUsers() >= 0);
		assertTrue(response.getNumUsersWithAllRoles() >= 0);
		assertTrue(response.getNumUsersWithNoRoles() >= 0);

		// Test Exception
		when(fa.getStats()).thenThrow(new IOException());
		response = adminController.getStats();
		assertNull(response);
	}

	/**
	 * Test GET /users
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetUsers() throws IOException {
		// Mock
		when(fa.getUsersAndRoles()).thenReturn(usersAndRoles);
		when(fa.getUsers()).thenCallRealMethod();

		// Test
		Set<String> response = roleManagementController.getUsers();

		// Verify
		assertTrue(users.equals(response));

		// Test Exception
		when(fa.getUsers()).thenThrow(new IOException());
		response = roleManagementController.getUsers();
		assertNull(response);
	}

	/**
	 * Test GET /users/{userid}/roles
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetRoles() throws IOException {
		String user = "dummy";

		// Mock
		when(fa.getUsersAndRoles()).thenReturn(usersAndRoles);
		when(fa.userExists(user)).thenCallRealMethod();
		when(fa.getUsers()).thenCallRealMethod();
		when(fa.getRolesForUser(user)).thenCallRealMethod();

		// Test
		List<String> response = roleManagementController.getRoles(user);

		// Verify
		assertTrue(roles.equals(response));

		// Test Exception
		when(fa.getRolesForUser(user)).thenThrow(new IOException());
		response = roleManagementController.getRoles(user);
		assertNull(response);
	}

	/**
	 * Test GET /users/roles
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetUsersAndRoles() throws IOException {

		// Mock
		when(fa.getUsersAndRoles()).thenReturn(usersAndRoles);

		// Test
		Map<String, String> response = roleManagementController.getUsersAndRoles();

		// Verify
		assertTrue(usersAndRoles.equals(response));

		// Test Exception
		when(fa.getUsersAndRoles()).thenThrow(new IOException());
		response = roleManagementController.getUsersAndRoles();
		assertNull(response);
	}

	/**
	 * Test POST /users
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAddUsers() throws IOException {
		String user = "testuser";

		// Mock
		when(fa.userExists(user)).thenReturn(false);
		HashMap<String, String> usersToAdd = new HashMap<String, String>();
		usersToAdd.put(user, user + ":");
		Mockito.doNothing().when(fa).addUsers(usersToAdd);

		// Test - New User
		Map<String, List<String>> response = roleManagementController.addUsers(new ArrayList<String>(Arrays.asList(user)));

		// Verify
		assertTrue(response.get("Successes").size() == 1);
		assertTrue(response.get("Successes").contains("User '" + user + "' inserted with no roles."));

		// Test - Existing User
		when(fa.userExists(user)).thenReturn(true);
		response = roleManagementController.addUsers(new ArrayList<String>(Arrays.asList(user)));
		assertTrue(response.get("Failures").size() == 1);
		assertTrue(response.get("Failures").contains("User '" + user + "' already exists!"));

		// Test Exception
		when(fa.userExists(user)).thenThrow(new IOException());
		response = roleManagementController.addUsers(new ArrayList<String>(Arrays.asList(user)));
		assertNull(response);
	}

	/**
	 * Test POST /users/roles
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAddUsersAndRoles() throws IOException {
		String user = "testuser";
		String role = "testrole";

		// Mock
		when(fa.userExists(user)).thenReturn(false);
		HashMap<String, String> usersAndRolesToAdd = new HashMap<String, String>();
		usersAndRolesToAdd.put(user, role);
		Mockito.doNothing().when(fa).addUsers(usersAndRolesToAdd);

		// Test - New User
		Map<String, List<String>> response = roleManagementController.addUsersAndRoles(usersAndRolesToAdd);

		// Verify
		assertTrue(response.get("Successes").size() == 1);
		assertTrue(response.get("Successes").contains("User '" + user + "' inserted with roles: " + role));

		// Test - Existing User
		when(fa.userExists(user)).thenReturn(true);
		response = roleManagementController.addUsersAndRoles(usersAndRolesToAdd);
		assertTrue(response.get("Failures").size() == 1);
		assertTrue(response.get("Failures").contains("User '" + user + "' already exists!"));

		// Test Exception
		when(fa.userExists(user)).thenThrow(new IOException());
		response = roleManagementController.addUsersAndRoles(usersAndRolesToAdd);
		assertNull(response);
	}

	/**
	 * Test PUT /users/{userid}/roles
	 * 
	 * @throws IOException
	 */
	@Test
	public void testUpdateRolesForUser() throws IOException {
		String user = "testuser";
		List<String> role = Arrays.asList("testrole");

		// Mock
		Mockito.doNothing().when(fa).updateUserRoles(user, role);
		when(fa.getRolesForUser(user)).thenReturn(role);

		// Test - User Exists
		when(fa.userExists(user)).thenReturn(true);
		Map<String, String> response = roleManagementController.updateRolesForUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' updated with roles: " + role));

		// Test - User Does Not Exist
		when(fa.userExists(user)).thenReturn(false);
		response = roleManagementController.updateRolesForUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' does not exist!"));

		// Test Exception
		when(fa.userExists(user)).thenThrow(new IOException());
		response = roleManagementController.updateRolesForUser(user, role);
		assertTrue(response.get("Status").contains("Exception: null"));
	}

	/**
	 * Test DELETE /users/{userid}
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteUser() throws Exception {
		String user = "orfrf";

		// Mock
		when(fa.getUsersAndCredentialsAndRoles()).thenReturn(usersAndCredentialsAndRoles);
		Mockito.doNothing().when(fa).removeUser(user);

		// Test - User Exists
		when(fa.userExists(user)).thenReturn(true);
		Map<String, String> response = roleManagementController.deleteUser(user);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' deleted."));

		// Test - User Does Not Exist
		when(fa.userExists(user)).thenReturn(false);
		response = roleManagementController.deleteUser(user);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' does not exist!"));

		// Test Exception
		when(fa.userExists(user)).thenThrow(new IOException());
		response = roleManagementController.deleteUser(user);
		assertTrue(response.get("Status").contains("Exception: null"));
	}

	/**
	 * Test DELETE /users/{userid}/roles
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteAllRolesFromUser() throws IOException {
		String user = "testuser";

		// Mock
		Mockito.doNothing().when(fa).removeAllRoles(user);

		// Test - User Exists
		when(fa.userExists(user)).thenReturn(true);
		Map<String, String> response = roleManagementController.deleteAllRolesFromUser(user);

		// Verify
		assertTrue(response.get("Status").contains("All roles for user '" + user + "' deleted."));

		// Test - User Does Not Exist
		when(fa.userExists(user)).thenReturn(false);
		response = roleManagementController.deleteAllRolesFromUser(user);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' does not exist!"));

		// Test Exception
		when(fa.userExists(user)).thenThrow(new IOException());
		response = roleManagementController.deleteAllRolesFromUser(user);
		assertTrue(response.get("Status").contains("Exception: null"));
	}

	/**
	 * Test DELETE /users/{userid}/roles/{role}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteSingleRoleFromUser() throws IOException {
		String user = "testuser";
		String role = "testrole";

		// Mock
		Mockito.doNothing().when(fa).removeRole(user, role);

		// Test - User And Role Exists
		when(fa.userExists(user)).thenReturn(true);
		when(fa.roleExists(user, role)).thenReturn(true);
		Map<String, String> response = roleManagementController.deleteRoleFromUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("Role '" + role + "' deleted for user '" + user + "'"));

		// Test - User Exists, Role Does Not Exist
		when(fa.userExists(user)).thenReturn(true);
		when(fa.roleExists(user, role)).thenReturn(false);
		response = roleManagementController.deleteRoleFromUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("Role '" + role + "' does not exist for user '" + user + "'"));

		// Test - User Does Not Exist
		when(fa.userExists(user)).thenReturn(false);
		when(fa.roleExists(user, role)).thenReturn(false);
		response = roleManagementController.deleteRoleFromUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' does not exist!"));

		// Test Exception
		when(fa.userExists(user)).thenThrow(new IOException());
		response = roleManagementController.deleteRoleFromUser(user, role);
		assertTrue(response.get("Status").contains("Exception: null"));
	}

	/**
	 * Test POST /verification
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAuthenticateUser() throws IOException {
		Map<String, String> body = new HashMap<String, String>();
		String username = "citester";
		String credential = "test4life";

		// Mock - User is empty
		when(ldapClient.getAuthenticationDecision(null, credential)).thenCallRealMethod();

		// Test
		body.put("username", null);
		body.put("credential", credential);
		Boolean response = authenticationController.authenticateUserByUserPass(body);

		// Verify
		assertFalse(response);

		// Mock - Non-override space
		ReflectionTestUtils.setField(ldapClient, "SPACE", "dev");
		when(ldapClient.getAuthenticationDecision(username, credential)).thenCallRealMethod();

		// Test
		body.put("username", username);
		body.put("credential", credential);
		response = authenticationController.authenticateUserByUserPass(body);

		// Verify
		assertFalse(response);

		// Mock
		ReflectionTestUtils.setField(ldapClient, "SPACE", "int");
		ReflectionTestUtils.setField(ldapClient, "TEST_PZTESTINTEGRATION_USER", "citester");
		ReflectionTestUtils.setField(ldapClient, "TEST_PZTESTINTEGRATION_CRED", "test4life");
		ReflectionTestUtils.setField(ldapClient, "TEST_BEACHFRONT_USER", "bfuser");
		ReflectionTestUtils.setField(ldapClient, "TEST_BEACHFRONT_USER", "bfpass");
		when(ldapClient.getAuthenticationDecision(username, credential)).thenCallRealMethod();

		// Test
		body.put("username", username);
		body.put("credential", credential);
		response = authenticationController.authenticateUserByUserPass(body);

		// Verify
		assertTrue(response);

		// Test Exception
		when(ldapClient.getAuthenticationDecision(username, credential)).thenThrow(new RuntimeException());
		response = authenticationController.authenticateUserByUserPass(body);
		assertFalse(response);
	}
}