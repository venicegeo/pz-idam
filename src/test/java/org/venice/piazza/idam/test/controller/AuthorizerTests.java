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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.venice.piazza.idam.authz.throttle.ThrottleAuthorizer;
import org.venice.piazza.idam.data.MongoAccessor;

import com.mongodb.MongoException;

import model.response.AuthResponse;
import model.security.authz.AuthorizationCheck;
import model.security.authz.Permission;
import util.PiazzaLogger;

/**
 * Tests authorizers and their logic
 * 
 * @author Patrick.Doody
 *
 */
public class AuthorizerTests {
	@Mock
	private MongoAccessor accessor;
	@Mock
	private PiazzaLogger pzLogger;
	@InjectMocks
	private ThrottleAuthorizer throttleAuthorizer;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);
	}

	/**
	 * Tests throttling permission checks against various requests
	 */
	@Test
	public void testThrottlingAuthorizer() {
		// Mock an Auth Check
		AuthorizationCheck mockCheck = new AuthorizationCheck();
		mockCheck.setUsername("tester");

		// Test endpoints that are not throttable
		mockCheck.setAction(new Permission("GET", "data"));
		AuthResponse response = throttleAuthorizer.canUserPerformAction(mockCheck);
		assertTrue(response.isAuthSuccess.equals(true));
		mockCheck.setAction(new Permission("GET", "event"));
		response = throttleAuthorizer.canUserPerformAction(mockCheck);
		assertTrue(response.isAuthSuccess.equals(true));

		// Test POST methods where the user is not throttled.
		when(accessor.getInvocationsForUserThrottle("tester", model.security.authz.Throttle.Component.JOB)).thenReturn(new Integer(5));
		mockCheck.setAction(new Permission("POST", "data"));
		response = throttleAuthorizer.canUserPerformAction(mockCheck);
		assertTrue(response.isAuthSuccess.equals(true));
		mockCheck.setAction(new Permission("POST", "job"));
		response = throttleAuthorizer.canUserPerformAction(mockCheck);
		assertTrue(response.isAuthSuccess.equals(true));

		// Test Jobs where the user is throttled due to excessive Jobs
		when(accessor.getInvocationsForUserThrottle("tester", model.security.authz.Throttle.Component.JOB))
				.thenReturn(new Integer(10000000));
		mockCheck.setAction(new Permission("POST", "data"));
		response = throttleAuthorizer.canUserPerformAction(mockCheck);
		assertTrue(response.isAuthSuccess.equals(false));
		assertTrue(response.getDetails().toString().contains("exceeded"));

		// Test when exceptions are thrown - throttles should be permissive (don't block if the DB goes down, or
		// something)
		when(accessor.getInvocationsForUserThrottle("tester", model.security.authz.Throttle.Component.JOB))
				.thenThrow(new MongoException("Oops"));
		mockCheck.setAction(new Permission("POST", "data"));
		response = throttleAuthorizer.canUserPerformAction(mockCheck);
		assertTrue(response.isAuthSuccess.equals(true));
	}

}
