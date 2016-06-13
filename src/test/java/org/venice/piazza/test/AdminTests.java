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

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.venice.piazza.security.controller.SecurityController;
import org.venice.piazza.security.data.FileAccessor;
import org.venice.piazza.security.data.Stats;

/**
 * Tests the Admin controller.
 * 
 * @author Patrick.Doody
 *
 */
public class AdminTests {

	@Mock
	private FileAccessor fa;

	@InjectMocks
	private SecurityController securityController;

	private Stats stats;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		// Mock a Service to use
		stats = new Stats(1, 2, 3, 4);
	}

	/**
	 * Tests root endpoint
	 */
	@Test
	public void testGetHealthCheck() {
		String result = securityController.getHealthCheck();
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
		when(fa.getStats()).thenReturn(stats);

		// Test
		Stats response = securityController.getStats();

		// Verify
		assertTrue(response.getNumRoles() >= 0);
		assertTrue(response.getNumUsers() >= 0);
		assertTrue(response.getNumUsersWithAllRoles() >= 0);
		assertTrue(response.getNumUsersWithNoRoles() >= 0);
	}
}