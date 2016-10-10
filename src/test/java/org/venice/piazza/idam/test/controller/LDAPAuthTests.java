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
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.venice.piazza.idam.authn.LDAPAuthenticator;
import util.PiazzaLogger;

public class LDAPAuthTests {

	@Mock
	private PiazzaLogger logger;

	@InjectMocks
	private LDAPAuthenticator ldapAuthenticator;
	
	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
		
		ReflectionTestUtils.setField(ldapAuthenticator, "testPzTestIntegrationUser", "citester");
		ReflectionTestUtils.setField(ldapAuthenticator, "testPzTestIntegrationCred", "test4life");
		ReflectionTestUtils.setField(ldapAuthenticator, "testBeachFrontUser", "bfuser");
		ReflectionTestUtils.setField(ldapAuthenticator, "testBeachFrontCred", "bfpass");
		ReflectionTestUtils.setField(ldapAuthenticator, "ldapUserDN", "invalid");		
		ReflectionTestUtils.setField(ldapAuthenticator, "ldapURL", "invalid");
		ReflectionTestUtils.setField(ldapAuthenticator, "ldapCtxFactory", "invalid");
	}
	
	@Test
	public void testGetAuthenticationDecisionUserPass() {
		
		// (1) Username is null, credential is null
		assertFalse(ldapAuthenticator.getAuthenticationDecision(null, "mypass").getAuthenticated());
		assertFalse(ldapAuthenticator.getAuthenticationDecision("myuser", null).getAuthenticated());

		// (2a) Username, credential are non-null, INT override space, BF user
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "int");
		assertTrue(ldapAuthenticator.getAuthenticationDecision("bfuser", "bfpass").getAuthenticated());
		
		// (2b) Username, credential are non-null, not an override space
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "dev");
		assertFalse(ldapAuthenticator.getAuthenticationDecision("bfuser", "bfpass").getAuthenticated());
		
		// (2c) Username, credential are non-null, TEST override space, BF user fails
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "test");
		assertFalse(ldapAuthenticator.getAuthenticationDecision("bfusername", "bfpass").getAuthenticated());
		
		// (2d) Username, credential are non-null, PROD override space, BF user fails
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "prod");
		assertFalse(ldapAuthenticator.getAuthenticationDecision("bfuser", "bfpassed").getAuthenticated());
		
		// (2e) Username, credential are non-null, STAGE override space, BF user
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "stage");
		assertTrue(ldapAuthenticator.getAuthenticationDecision("bfuser", "bfpass").getAuthenticated());		
		
		// (3a) Username, credential are non-null, override space, PZTEST user
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "int");		
		assertTrue(ldapAuthenticator.getAuthenticationDecision("citester", "test4life").getAuthenticated());
		
		// (3b) Username, credential are non-null, override space, PZTEST user fails
		ReflectionTestUtils.setField(ldapAuthenticator, "space", "int");		
		assertFalse(ldapAuthenticator.getAuthenticationDecision("citester", "test5life").getAuthenticated());		
		
		// (4) Username, credential are non-null, LDAP
		assertFalse(ldapAuthenticator.getAuthenticationDecision("notauser", "notapass").getAuthenticated());
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testGetAuthenticationDecisionPKI() {
		ldapAuthenticator.getAuthenticationDecision("testpem");
	}
}