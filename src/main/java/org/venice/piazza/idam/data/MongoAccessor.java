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
package org.venice.piazza.idam.data;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import util.PiazzaLogger;

@Component
public class MongoAccessor {

	@Autowired
	private PiazzaLogger pzLogger;

	@Value("${vcap.services.pz-mongodb.credentials.uri}")
	private String mongoHost;

	@Value("${vcap.services.pz-mongodb.credentials.database}")
	private String mongoDBName;

	@Value("${mongo.db.collection.name}")
	private String mongoCollectionName;

	private DB mongoDatabase;
	private MongoClient mongoClient;
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MongoAccessor.class);
	
	private static final String USERNAME = "username";
	private static final String UUID = "uuid";
	
	@PostConstruct
	private void initialize() {
		try {
			mongoClient = new MongoClient(new MongoClientURI(mongoHost)); 
			mongoDatabase = mongoClient.getDB(mongoDBName); //NOSONAR
		} catch (Exception exception) {
			String error = String.format("Error Contacting Mongo Host %s: %s", mongoHost, exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, PiazzaLogger.ERROR);
		}
	}

	@PreDestroy
	private void close() {
		mongoClient.close();
	}

	public void update(String username, String uuid) {
		BasicDBObject newObj = new BasicDBObject();
		newObj.put(USERNAME, username);
		newObj.put(UUID, uuid);
		mongoDatabase.getCollection(mongoCollectionName).update(new BasicDBObject().append(USERNAME, username), newObj);
	}
	
	public void save(String username, String uuid) {
		mongoDatabase.getCollection(mongoCollectionName).insert(new BasicDBObject().append(USERNAME, username).append(UUID, uuid));
	}

	public boolean isAPIKeyValid(String uuid) {
		DBObject obj = mongoDatabase.getCollection(mongoCollectionName).findOne(new BasicDBObject(UUID, uuid));
		if (obj != null) {
			return true;
		}
		return false;
	}

	public String getUsername(String uuid) {
		DBObject obj = mongoDatabase.getCollection(mongoCollectionName).findOne(new BasicDBObject(UUID, uuid));
		if (obj != null && obj.containsField(USERNAME)) {
			return obj.get(USERNAME).toString();
		}
		return null;
	}

	public String getUuid(String username) {
		DBObject obj = mongoDatabase.getCollection(mongoCollectionName).findOne(new BasicDBObject(USERNAME, username));
		if (obj != null && obj.containsField(UUID)) {
			return obj.get(UUID).toString();
		}
		return null;
	}
}