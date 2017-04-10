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
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mongojack.DBQuery.Query;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.springframework.test.util.ReflectionTestUtils;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.ApiKey;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

import util.PiazzaLogger;

public class MongoAccessorTests {

	@Mock
	private PiazzaLogger logger;

	@Mock
	private DB mongoDatabase;

	@Mock
	private DBCollection dbCollection;

	@Mock
	private JacksonDBCollection<ApiKey, String> apiCollection;

	@Spy
	@InjectMocks
	private MongoAccessor mongoAccessor;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		Mockito.doReturn(apiCollection).when(mongoAccessor).getApiKeyCollection();

		ReflectionTestUtils.setField(mongoAccessor, "DATABASE_HOST", "test");
		ReflectionTestUtils.setField(mongoAccessor, "DATABASE_NAME", "test");
		ReflectionTestUtils.setField(mongoAccessor, "API_KEY_COLLECTION_NAME", "collectionname");
		ReflectionTestUtils.setField(mongoAccessor, "KEY_INACTIVITY_THESHOLD_MS", new Long(5000));
		ReflectionTestUtils.setField(mongoAccessor, "KEY_EXPIRATION_DURATION_MS", new Long(10000));
	}

	@Test
	public void testUpdate() {
		BasicDBObject newObj = new BasicDBObject();
		newObj.put("username", "myuser");
		newObj.put("uuid", "myuuid");

		BasicDBObject toUpdate = new BasicDBObject().append("username", "myuser");

		when(mongoDatabase.getCollection(eq("collectionname"))).thenReturn(dbCollection);
		when(dbCollection.update(refEq(toUpdate), refEq(newObj))).thenReturn(null);

		mongoAccessor.updateApiKey("myuser", "myuuid");
	}

	@Test
	public void testSave() {
		BasicDBObject toInsert = new BasicDBObject().append("username", "myuser").append("uuid", "myuuid");

		when(mongoDatabase.getCollection(eq("collectionname"))).thenReturn(dbCollection);
		when(dbCollection.insert(refEq(toInsert))).thenReturn(null);

		mongoAccessor.createApiKey("myuser", "myuuid");
	}

	@Test
	public void testIsAPIKeyValid() {
		// Mock
		ApiKey apiKey = new ApiKey("myuuid", "tester", System.currentTimeMillis(), System.currentTimeMillis() + 50000);

		// (1) API Key is invalid
		Mockito.doReturn(null).when(apiCollection).findOne(Mockito.any(Query.class));
		assertFalse(mongoAccessor.isApiKeyValid("myuuid"));

		// (2) API Key is valid
		Mockito.doReturn(apiKey).when(apiCollection).findOne(Mockito.any(Query.class));
		assertTrue(mongoAccessor.isApiKeyValid("myuuid"));

		// (3) API Key is expired
		apiKey.setExpiresOn(System.currentTimeMillis() - 50000);
		assertFalse(mongoAccessor.isApiKeyValid("myuuid"));

		// (4) API Key is inactive
		apiKey.setExpiresOn(System.currentTimeMillis() + 50000);
		apiKey.setLastUsedOn(System.currentTimeMillis() - 50000);
		assertFalse(mongoAccessor.isApiKeyValid("myuuid"));
	}

	@Test
	public void testGetUsername() {
		// Mock
		ApiKey apiKey = new ApiKey();
		apiKey.setUserName("tester");
		apiKey.setApiKey("myuuid");

		// (1) Exists
		Mockito.doReturn(apiKey).when(apiCollection).findOne(Mockito.any(Query.class));
		String userName = mongoAccessor.getUsername("myuuid");
		assertTrue(apiKey.getUserName().equals(userName));

		// (2) Does not Exist
		Mockito.doReturn(null).when(apiCollection).findOne(Mockito.any(Query.class));
		userName = mongoAccessor.getUsername("junkuuid");
		assertTrue(userName == null);
	}

}