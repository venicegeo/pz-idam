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
	private PiazzaLogger logger;

	@Value("${vcap.services.pz-mongodb.credentials.uri}")
	private String mongoHost;

	@Value("${vcap.services.pz-mongodb.credentials.database}")
	private String mongoDBName;

	@Value("${mongo.db.collection.name}")
	private String mongoCollectionName;

	private DB mongoDatabase;

	@PostConstruct
	private void initialize() {
		try {
			//revisit this code to close the client or rewrite mongoclient instantiation
			mongoDatabase = new MongoClient(new MongoClientURI(mongoHost)).getDB(mongoDBName); //NOSONAR
		} catch (Exception ex) {
			logger.log(String.format("Error Contacting Mongo Host %s: %s", mongoHost, ex.getMessage()),
					PiazzaLogger.ERROR);
		}
	}

	/*
	@PreDestroy
	private void close() {
		mongoDatabase.getMongo().close();
	}
	*/

	public void update(String username, String uuid) {
		BasicDBObject newObj = new BasicDBObject();
		newObj.put("username", username);
		newObj.put("uuid", uuid);
		mongoDatabase.getCollection(mongoCollectionName).update(new BasicDBObject().append("username", username), newObj);
	}
	
	public void save(String username, String uuid) {
		mongoDatabase.getCollection(mongoCollectionName).insert(new BasicDBObject().append("username", username).append("uuid", uuid));
	}

	public boolean isAPIKeyValid(String uuid) {
		DBObject obj = mongoDatabase.getCollection(mongoCollectionName).findOne(new BasicDBObject("uuid", uuid));
		if (obj != null) {
			return true;
		}
		return false;
	}

	public String getUsername(String uuid) {
		DBObject obj = mongoDatabase.getCollection(mongoCollectionName).findOne(new BasicDBObject("uuid", uuid));
		if (obj != null && obj.containsField("username")) {
			return obj.get("username").toString();
		}
		return null;
	}

	public String getUuid(String username) {
		DBObject obj = mongoDatabase.getCollection(mongoCollectionName).findOne(new BasicDBObject("username", username));
		if (obj != null && obj.containsField("uuid")) {
			return obj.get("uuid").toString();
		}
		return null;
	}
}
