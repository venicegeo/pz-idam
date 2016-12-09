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

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.authn.PiazzaAuthenticator;
import org.venice.piazza.idam.controller.AdminController;
import org.venice.piazza.idam.controller.AuthController;
import org.venice.piazza.idam.data.MongoAccessor;

import model.response.AuthResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.UUIDResponse;
import model.security.authz.UserProfile;
import util.PiazzaLogger;
import util.UUIDFactory;

public class ControllerTests {

	@Mock
	private Environment env;

	@Mock
	private RestTemplate restTemplate;

	@Mock
	private MongoAccessor mongoAccessor;

	@Mock
	private UUIDFactory uuidFactory;

	@Mock
	private HttpServletRequest request;

	@Mock
	private PiazzaLogger logger;

	@Mock
	private PiazzaAuthenticator piazzaAuthenticator;

	@InjectMocks
	private AdminController adminController;

	@InjectMocks
	private AuthController authenticationController;

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

	@Test
	public void testGetAdminStats() {
		when(env.getActiveProfiles()).thenReturn(new String[] { "geoaxis" });
		String result = adminController.getAdminStats();
		assertTrue("{ \"profiles\":\"geoaxis\" }".equals(result));
	}

	@Test
	public void testAuthenticateUserByUUID() {
		// (1) Mock uuid is missing

		// Test
		ResponseEntity<AuthResponse> response = authenticationController.authenticateApiKey(new HashMap<String, String>());

		// Verify
		assertTrue(response.getBody() instanceof AuthResponse);
		assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue() == false);

		// (2) Mock uuid is present in request, but missing
		Map<String, String> body = new HashMap<String, String>();
		body.put("uuid", "1234");
		when(mongoAccessor.isApiKeyValid("1234")).thenReturn(false);

		UserProfile mockProfile = new UserProfile();
		mockProfile.setUsername("bsmith");
		when(mongoAccessor.getUserProfileByApiKey(Mockito.eq("1234"))).thenReturn(mockProfile);

		// Test
		response = authenticationController.authenticateApiKey(body);

		// Verify
		assertTrue(response.getBody() instanceof AuthResponse);
		assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue() == false);

		// (3) Mock uuid is present in request, and valid
		when(mongoAccessor.getUserProfileByApiKey(Mockito.eq("1234"))).thenReturn(mockProfile);
		when(mongoAccessor.isApiKeyValid("1234")).thenReturn(true);

		// Test
		response = authenticationController.authenticateApiKey(body);

		// Verify
		assertTrue(response.getBody() instanceof AuthResponse);
		assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue());
		assertTrue(((AuthResponse) response.getBody()).getUserProfile().getUsername().equals("bsmith"));

		// (4) Mock Exception thrown
		when(mongoAccessor.getUserProfileByApiKey(Mockito.eq("1234"))).thenThrow(new RuntimeException("My Bad"));
		Mockito.doNothing().when(logger).log(Mockito.anyString(), Mockito.any());

		// Test
		response = authenticationController.authenticateApiKey(body);

		// Verify
		assertTrue(response.getBody() instanceof AuthResponse);
		assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue() == false);
	}

	@Test
	public void testRetrieveUUID() {

		// (1) Mock - Header is missing
		when(request.getHeader("Authorization")).thenReturn(null);

		// Test
		ResponseEntity<PiazzaResponse> response = authenticationController.retrieveUUID();

		// Verify
		assertTrue(response.getBody() instanceof ErrorResponse);
		assertTrue(((ErrorResponse) (response.getBody())).message.contains("Authentication failed for user"));

		// (2) Mock - Header present, Auth fails
		when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
		UserProfile mockProfile = new UserProfile();
		mockProfile.setUsername("testuser");
		when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass"))
				.thenReturn(new AuthResponse(false, mockProfile));

		// Test
		response = authenticationController.retrieveUUID();

		// Verify
		assertTrue(response.getBody() instanceof ErrorResponse);

		// (3) Mock - Header present, BASIC Auth succeeds, new key
		when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass"))
				.thenReturn(new AuthResponse(true, mockProfile));
		when(uuidFactory.getUUID()).thenReturn("1234");
		when(mongoAccessor.getApiKey("testuser")).thenReturn(null);
		Mockito.doNothing().when(mongoAccessor).createApiKey("testuser", "1234");

		// Test
		response = authenticationController.retrieveUUID();

		// Verify
		assertTrue(response.getBody() instanceof UUIDResponse);
		assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

		// (4) Mock - Header present, BASIC Auth succeeds, replacing key with new
		when(mongoAccessor.getApiKey("testuser")).thenReturn("4321");
		Mockito.doNothing().when(mongoAccessor).updateApiKey("testuser", "1234");

		// Test
		response = authenticationController.retrieveUUID();

		// Verify
		assertTrue(response.getBody() instanceof UUIDResponse);
		assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

		// (5) Mock - Header present, PKI Auth succeeds, replacing key with new
		when(request.getHeader("Authorization"))
				.thenReturn("Basic LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tIHBlbVRlc3QgLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLTo=");
		when(piazzaAuthenticator.getAuthenticationDecision("-----BEGIN CERTIFICATE----- pemTest -----END CERTIFICATE-----"))
				.thenReturn(new AuthResponse(true, mockProfile));
		when(uuidFactory.getUUID()).thenReturn("1234");
		when(mongoAccessor.getApiKey("testuser")).thenReturn("4321");
		Mockito.doNothing().when(mongoAccessor).updateApiKey("testuser", "1234");

		// Test
		response = authenticationController.retrieveUUID();

		// Verify
		assertTrue(response.getBody() instanceof UUIDResponse);
		assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

		// (6) Mock Exception
		when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
		when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass")).thenThrow(new RuntimeException("My Bad"));
		Mockito.doNothing().when(logger).log(Mockito.anyString(), Mockito.any());

		// Test
		response = authenticationController.retrieveUUID();

		// Verify
		assertTrue(response.getBody() instanceof ErrorResponse);
		assertTrue(((ErrorResponse) (response.getBody())).message.equals("Error retrieving UUID: My Bad"));
	}
}