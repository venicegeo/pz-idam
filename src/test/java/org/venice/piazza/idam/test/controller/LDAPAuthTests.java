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
		
		ReflectionTestUtils.setField(ldapAuthenticator, "SPACE", "int");
		ReflectionTestUtils.setField(ldapAuthenticator, "TEST_PZTESTINTEGRATION_USER", "citester");
		ReflectionTestUtils.setField(ldapAuthenticator, "TEST_PZTESTINTEGRATION_CRED", "test4life");
		ReflectionTestUtils.setField(ldapAuthenticator, "TEST_BEACHFRONT_USER", "bfuser");
		ReflectionTestUtils.setField(ldapAuthenticator, "TEST_BEACHFRONT_CRED", "bfpass");
		ReflectionTestUtils.setField(ldapAuthenticator, "LDAP_USER_DN", "invalid");		
		ReflectionTestUtils.setField(ldapAuthenticator, "LDAP_URL", "invalid");
		ReflectionTestUtils.setField(ldapAuthenticator, "LDAP_CTX_FACTORY", "invalid");
	}
	
	@Test
	public void testGetAuthenticationDecisionUserPass() {
		
		// (1) Username is null, credential is null
		assertFalse(ldapAuthenticator.getAuthenticationDecision(null, "mypass"));
		assertFalse(ldapAuthenticator.getAuthenticationDecision("myuser", null));

		// (2) Username, credential are non-null, override space, BF user
		assertTrue(ldapAuthenticator.getAuthenticationDecision("bfuser", "bfpass"));
		
		// (3) Username, credential are non-null, override space, PZTEST user
		assertTrue(ldapAuthenticator.getAuthenticationDecision("citester", "test4life"));
		
		// (4) Username, credential are non-null, LDAP
		assertFalse(ldapAuthenticator.getAuthenticationDecision("notauser", "notapass"));
	}
	
	@Test(expected=UnsupportedOperationException.class)
	public void testGetAuthenticationDecisionPKI() {
		ldapAuthenticator.getAuthenticationDecision("testpem");
	}
}