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
import org.springframework.test.util.ReflectionTestUtils;
import org.venice.piazza.idam.data.MongoAccessor;

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

	@InjectMocks
	private MongoAccessor mongoAccessor;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
		MockitoAnnotations.initMocks(this);

		ReflectionTestUtils.setField(mongoAccessor, "mongoHost", "test");
		ReflectionTestUtils.setField(mongoAccessor, "mongoDBName", "test");
		ReflectionTestUtils.setField(mongoAccessor, "API_KEY_COLLECTION_NAME", "collectionname");
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
		BasicDBObject toFind = new BasicDBObject("uuid", "myuuid");

		DBObject dbObj = Mockito.mock(DBObject.class);

		when(mongoDatabase.getCollection(eq("collectionname"))).thenReturn(dbCollection);

		// (1) API Key is valid
		when(dbCollection.findOne(refEq(toFind))).thenReturn(dbObj);
		assertTrue(mongoAccessor.isApiKeyValid("myuuid"));

		// (2) API Key is invalid
		when(dbCollection.findOne(refEq(toFind))).thenReturn(null);
		assertFalse(mongoAccessor.isApiKeyValid("myuuid"));
	}

	@Test
	public void testGetUsername() {
		BasicDBObject toFind = new BasicDBObject("uuid", "myuuid");
		DBObject dbObj = Mockito.mock(DBObject.class);

		when(mongoDatabase.getCollection(eq("collectionname"))).thenReturn(dbCollection);

		// (1) Username is valid
		when(dbCollection.findOne(refEq(toFind))).thenReturn(dbObj);
		when(dbObj.containsField("username")).thenReturn(true);
		when(dbObj.get("username")).thenReturn("bsmith");
		assertTrue(mongoAccessor.getUsername("myuuid").equals("bsmith"));

		// (2) Username is invalid; record is present
		when(dbCollection.findOne(refEq(toFind))).thenReturn(dbObj);
		when(dbObj.containsField("username")).thenReturn(false);
		assertNull(mongoAccessor.getUsername("myuuid"));

		// (3) Username is invalid; record is missing
		when(dbCollection.findOne(refEq(toFind))).thenReturn(null);
		assertNull(mongoAccessor.getUsername("myuuid"));
	}

	@Test
	public void testGetUuid() {
		BasicDBObject toFind = new BasicDBObject("username", "myuser");
		DBObject dbObj = Mockito.mock(DBObject.class);

		when(mongoDatabase.getCollection(eq("collectionname"))).thenReturn(dbCollection);

		// (1) UUID is valid
		when(dbCollection.findOne(refEq(toFind))).thenReturn(dbObj);
		when(dbObj.containsField("uuid")).thenReturn(true);
		when(dbObj.get("uuid")).thenReturn("longuuidstring");
		assertTrue(mongoAccessor.getApiKey("myuser").equals("longuuidstring"));

		// (2) UUID is invalid, record is present
		when(dbCollection.findOne(refEq(toFind))).thenReturn(dbObj);
		when(dbObj.containsField("uuid")).thenReturn(false);
		assertNull(mongoAccessor.getApiKey("myuser"));

		// (3) UUID is invalid, record is missing
		when(dbCollection.findOne(refEq(toFind))).thenReturn(null);
		assertNull(mongoAccessor.getApiKey("myuser"));
	}
}