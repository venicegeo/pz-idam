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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.authn.GxAuthenticator;
import org.venice.piazza.idam.model.GxAuthNCertificateRequest;
import org.venice.piazza.idam.model.GxAuthNResponse;
import org.venice.piazza.idam.model.GxAuthNUserPassRequest;
import org.venice.piazza.idam.model.Principal;
import org.venice.piazza.idam.model.PrincipalItem;

public class GxAuthTests {

	@Mock
	private RestTemplate restTemplate;

	@InjectMocks
	private GxAuthenticator gxAuthenticator;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testGetAuthenticationDecisionUserPass() {

		// Mock Gx Service Call
		ReflectionTestUtils.setField(gxAuthenticator, "gxApiUrlAtnBasic", "https://geoaxis.api.com/atnrest/basic");
		ReflectionTestUtils.setField(gxAuthenticator, "gxBasicMechanism", "GxDisAus");
		ReflectionTestUtils.setField(gxAuthenticator, "gxBasicHostIdentifier", "//OAMServlet/disaususerprotected");

		GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername("bsmith");
		request.setPassword("mypass");
		request.setMechanism("GxDisAus");
		request.setHostIdentifier("//OAMServlet/disaususerprotected");

		GxAuthNResponse gxResponse = new GxAuthNResponse();
		gxResponse.setSuccessful(false);

		Mockito.doReturn(gxResponse).when(restTemplate).postForObject(eq("https://geoaxis.api.com/atnrest/basic"),
				refEq(request), eq(GxAuthNResponse.class));

		// Test
		boolean isAuthenticated = gxAuthenticator.getAuthenticationDecision("bsmith", "mypass").getAuthenticated();

		// Verify
		assertFalse(isAuthenticated);
	}

	@Test
	public void testGetAuthenticationDecisionPKI() {

		// Mock Gx Service Call
		ReflectionTestUtils.setField(gxAuthenticator, "gxApiUrlAtnCert", "https://geoaxis.api.com/atnrest/cert");

		String testPEMFormatted = "-----BEGIN CERTIFICATE-----\nthis\nis\njust\na\ntest\nyes\nit\nis\n-----END CERTIFICATE-----";
		String testPEM = "-----BEGIN CERTIFICATE----- this is just a test yes it is -----END CERTIFICATE-----";

		// Mock Request
		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(testPEMFormatted);
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");

		// (1) Mock Response - No PrincipalItems returned.
		Principal principal = new Principal();
		GxAuthNResponse gxResponse = new GxAuthNResponse();
		gxResponse.setSuccessful(false);
		gxResponse.setPrincipals(principal);

		// Test
		Mockito.doReturn(gxResponse).when(restTemplate).postForObject(eq("https://geoaxis.api.com/atnrest/cert"),
				refEq(request), eq(GxAuthNResponse.class));
		boolean isAuthenticated = gxAuthenticator.getAuthenticationDecision(testPEM).getAuthenticated();
		String username = gxAuthenticator.getAuthenticationDecision(testPEM).getUsername();

		// Verify
		assertFalse(isAuthenticated);
		assertNull(username);

		// (2) Mock Response - UID returned
		PrincipalItem principalItem = new PrincipalItem();
		principalItem.setName("UID");
		principalItem.setValue("testuser");

		principal.setPrincipal(Arrays.asList(principalItem));
		gxResponse.setPrincipals(principal);

		// Test
		Mockito.doReturn(gxResponse).when(restTemplate).postForObject(eq("https://geoaxis.api.com/atnrest/cert"),
				refEq(request), eq(GxAuthNResponse.class));
		isAuthenticated = gxAuthenticator.getAuthenticationDecision(testPEM).getAuthenticated();

		// Verify
		assertFalse(isAuthenticated);

		// (3) Mock Response - PrincipalItems with no UID returned.
		principalItem = new PrincipalItem();
		principalItem.setName("CN");
		principalItem.setValue("a CN string");

		principal.setPrincipal(Arrays.asList(principalItem));
		gxResponse.setPrincipals(principal);

		// Test
		Mockito.doReturn(gxResponse).when(restTemplate).postForObject(eq("https://geoaxis.api.com/atnrest/cert"),
				refEq(request), eq(GxAuthNResponse.class));
		isAuthenticated = gxAuthenticator.getAuthenticationDecision(testPEM).getAuthenticated();
		username = gxAuthenticator.getAuthenticationDecision(testPEM).getUsername();

		// Verify
		assertFalse(isAuthenticated);
		assertNull(username);
	}
}