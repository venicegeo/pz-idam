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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;

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
		ReflectionTestUtils.setField(gxAuthenticator, "GX_API_URL_ATN_BASIC", "https://geoaxis.api.com/atnrest/basic");
		
		GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername("bsmith");
		request.setPassword("mypass");
		request.setMechanism("GxDisAus");
		request.setHostIdentifier("//OAMServlet/disaususerprotected");
	
		GxAuthNResponse gxResponse = new GxAuthNResponse();
		gxResponse.setSuccessful(false);
				
		Mockito.doReturn(gxResponse).when(restTemplate).postForObject(eq("https://geoaxis.api.com/atnrest/basic"), refEq(request), eq(GxAuthNResponse.class));
		
		// Test
		boolean isAuthenticated = gxAuthenticator.getAuthenticationDecision("bsmith","mypass").getAuthenticated();
		
		// Verify
		assertFalse(isAuthenticated);
	}
	
	@Test
	public void testGetAuthenticationDecisionPKI() {
		
		// Mock Gx Service Call
		ReflectionTestUtils.setField(gxAuthenticator, "GX_API_URL_ATN_CERT", "https://geoaxis.api.com/atnrest/cert");
		
		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert("pemcertgoeshere");
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");
	
		GxAuthNResponse gxResponse = new GxAuthNResponse();
		gxResponse.setSuccessful(false);
				
		Mockito.doReturn(gxResponse).when(restTemplate).postForObject(eq("https://geoaxis.api.com/atnrest/cert"), refEq(request), eq(GxAuthNResponse.class));
		
		// Test
		boolean isAuthenticated = gxAuthenticator.getAuthenticationDecision("pemcertgoeshere").getAuthenticated();
		
		// Verify
		assertFalse(isAuthenticated);		
	}
}