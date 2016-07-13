package org.venice.piazza.security.data;

import javax.annotation.PostConstruct;

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
			mongoDatabase = new MongoClient(new MongoClientURI(mongoHost)).getDB(mongoDBName);
		} catch (Exception ex) {
			logger.log(String.format("Error Contacting Mongo Host %s: %s", mongoHost, ex.getMessage()),
					PiazzaLogger.ERROR);
		}
	}

	public void update(String username, String uuid) {
		BasicDBObject newObj = new BasicDBObject();
		newObj.put("username", username);
		newObj.put("uuid", uuid);
		mongoDatabase.getCollection(mongoCollectionName).update(new BasicDBObject().append("username", username), newObj);
	}
	
	public void save(String username, String uuid) {
		mongoDatabase.getCollection(mongoCollectionName).insert(new BasicDBObject().append("username", username).append("uuid", uuid));
	}

	public boolean getAuthenticationDecision(String uuid) {
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