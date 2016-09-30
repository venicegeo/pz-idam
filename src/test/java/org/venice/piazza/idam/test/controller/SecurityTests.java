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
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.authn.LDAPAuthenticator;
import org.venice.piazza.idam.controller.AdminController;
import org.venice.piazza.idam.controller.AuthenticationController;

public class SecurityTests {

	@Mock
	private LDAPAuthenticator ldapClient;

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private AdminController adminController;

	@InjectMocks
	private AuthenticationController authenticationController;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
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

//		// Mock
//		when(fa.getUsersAndRoles()).thenReturn(usersAndRoles);
//		when(fa.getUsers()).thenCallRealMethod();
//		when(fa.getNumRoles()).thenCallRealMethod();
//		when(fa.getNumUsersWithNoRoles()).thenCallRealMethod();
//		when(fa.getNumUsersWithAllRoles()).thenCallRealMethod();
//		when(fa.getStats()).thenCallRealMethod();
//
//		// Test
//		Stats response = adminController.getStats();
//
//		// Verify
//		assertTrue(response.getNumRoles() >= 0);
//		assertTrue(response.getNumUsers() >= 0);
//		assertTrue(response.getNumUsersWithAllRoles() >= 0);
//		assertTrue(response.getNumUsersWithNoRoles() >= 0);
//
//		// Test Exception
//		when(fa.getStats()).thenThrow(new IOException());
//		response = adminController.getStats();
//		assertNull(response);
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
		when(ldapClient.getAuthenticationDecision(null, credential, null)).thenCallRealMethod();

		// Test
		body.put("username", null);
		body.put("credential", credential);
		Boolean response = authenticationController.authenticateUserByUserPass(body);

		// Verify
		assertFalse(response);

		// Mock - Non-override space
		ReflectionTestUtils.setField(ldapClient, "SPACE", "dev");
		when(ldapClient.getAuthenticationDecision(username, credential, null)).thenCallRealMethod();

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
		when(ldapClient.getAuthenticationDecision(username, credential, null)).thenCallRealMethod();

		// Test
		body.put("username", username);
		body.put("credential", credential);
		response = authenticationController.authenticateUserByUserPass(body);

		// Verify
		assertTrue(response);

		// Test Exception
		when(ldapClient.getAuthenticationDecision(username, credential, null)).thenThrow(new RuntimeException());
		response = authenticationController.authenticateUserByUserPass(body);
		assertFalse(response);
	}
}