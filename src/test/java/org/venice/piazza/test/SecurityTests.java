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
package org.venice.piazza.test;

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
import org.apache.kafka.clients.producer.Producer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.security.controller.SecurityController;
import org.venice.piazza.security.data.FileAccessor;
import org.venice.piazza.security.data.LDAPClient;

public class SecurityTests {

	@Mock
	private FileAccessor fa;

	@Mock
	private LDAPClient ldapClient;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private SecurityController securityController;
	@Mock
	private Producer<String, String> producer;

	private Set<String> users = new HashSet<String>(Arrays.asList("yutzlejp", "beckerwg", "krasnebh", "mcmahojm",
			"orfrf", "naquinkj", "doodypc", "dionmr", "chambebj", "bardenbm", "clarksp", "smithpq", "duncanjl",
			"mauckaw", "smithcs", "lynummm", "gerlekmp", "baxtersh", "sanievsf", "baziledd", "brownjt", "citester"));

	private List<String> roles = new ArrayList<String>(
			Arrays.asList("abort", "access", "admin-stats", "delete-service", "execute-service", "get", "get-resource",
					"ingest", "list-service", "read-service", "register-service", "search-service", "update-service"));

	private Map<String, String> usersAndRoles = new HashMap<String, String>();

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		usersAndRoles.put("yutzlejp",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("beckerwg",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("krasnebh",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("mcmahojm",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("orfrf",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("naquinkj",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("doodypc",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("dionmr",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("chambebj",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("bardenbm",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("clarksp",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("smithpq",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("duncanjl",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("mauckaw",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("smithcs",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("lynummm",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("gerlekmp",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("baxtersh",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("sanievsf",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("baziledd",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("brownjt",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
		usersAndRoles.put("citester",
				"abort,access,admin-stats,delete-service,execute-service,get,get-resource,ingest,list-service,read-service,register-service,search-service,update-service");
	}

	/**
	 * Test GET /users
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetUsers() throws IOException {
		// Mock
		when(fa.getUsers()).thenReturn(users);

		// Test
		Set<String> response = securityController.getUsers();

		// Verify
		assertTrue(users.equals(response));

		// Test Exception
		// when(restTemplate.getForObject(anyString(),
		// eq(PiazzaResponse.class)))
		// .thenThrow(new RestClientException("No Service"));
	}

	/**
	 * Test GET /users/{userid}/roles
	 * 
	 * @throws IOException
	 */
	@Test
	public void testGetRoles() throws IOException {
		String user = "testuser";

		// Mock
		when(fa.getRolesForUser(user)).thenReturn(roles);

		// Test
		List<String> response = securityController.getRoles(user);

		// Verify
		assertTrue(roles.equals(response));

		// Test Exception
		// when(restTemplate.getForObject(anyString(),
		// eq(PiazzaResponse.class)))
		// .thenThrow(new RestClientException("No Service"));
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
		Map<String, String> response = securityController.getUsersAndRoles();

		// Verify
		assertTrue(usersAndRoles.equals(response));

		// Test Exception
		// when(restTemplate.getForObject(anyString(),
		// eq(PiazzaResponse.class)))
		// .thenThrow(new RestClientException("No Service"));
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
		Map<String, String> usersToAdd = new HashMap<String, String>();
		usersToAdd.put(user, user + ":");
		Mockito.doNothing().when(fa).addUsers(usersToAdd);

		// Test
		Map<String, List<String>> response = securityController.addUsers(new ArrayList<String>(Arrays.asList(user)));

		// Verify
		assertTrue(response.get("Successes").size() == 1);
		assertTrue(response.get("Successes").contains("User '" + user + "' inserted with no roles."));
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
		Map<String, String> usersAndRolesToAdd = new HashMap<String, String>();
		usersAndRolesToAdd.put(user, role);
		Mockito.doNothing().when(fa).addUsers(usersAndRolesToAdd);

		// Test
		Map<String, List<String>> response = securityController.addUsersAndRoles(usersAndRolesToAdd);

		// Verify
		assertTrue(response.get("Successes").size() == 1);
		assertTrue(response.get("Successes").contains("User '" + user + "' inserted with roles: " + role));
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
		when(fa.userExists(user)).thenReturn(true);
		Mockito.doNothing().when(fa).updateUserRoles(user, role);
		when(fa.getRolesForUser(user)).thenReturn(role);

		// Test
		Map<String, String> response = securityController.updateRolesForUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' updated with roles: " + role));
	}

	/**
	 * Test DELETE /users/{userid}
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeleteUser() throws IOException {
		String user = "testuser";

		// Mock
		when(fa.userExists(user)).thenReturn(true);
		Mockito.doNothing().when(fa).removeUser(user);

		// Test
		Map<String, String> response = securityController.deleteUser(user);

		// Verify
		assertTrue(response.get("Status").contains("User '" + user + "' deleted."));
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
		when(fa.userExists(user)).thenReturn(true);
		Mockito.doNothing().when(fa).removeAllRoles(user);

		// Test
		Map<String, String> response = securityController.deleteAllRolesFromUser(user);

		// Verify
		assertTrue(response.get("Status").contains("All roles for user '" + user + "' deleted."));
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
		when(fa.userExists(user)).thenReturn(true);
		when(fa.roleExists(user, role)).thenReturn(true);
		Mockito.doNothing().when(fa).removeRole(user, role);

		// Test
		Map<String, String> response = securityController.deleteRoleFromUser(user, role);

		// Verify
		assertTrue(response.get("Status").contains("Role '" + role + "' deleted for user '" + user + "'"));
	}

	/**
	 * Test POST /verification
	 * 
	 * @throws IOException
	 */
	@Test
	public void testAuthenticateUser() throws IOException {
		String user = "testuser";
		String credential = "credential";

		Map<String, String> body = new HashMap<String, String>();
		body.put("username", user);
		body.put("credential", credential);

		// Mock
		when(ldapClient.getAuthenticationDecision(user, credential)).thenReturn(true);

		// Test
		Boolean response = securityController.authenticateUser(body);

		// Verify
		assertTrue(response);
	}
}