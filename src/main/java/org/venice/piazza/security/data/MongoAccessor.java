package org.venice.piazza.security.data;

import javax.annotation.PostConstruct;

import org.mongojack.JacksonDBCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
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

	public void save(String username, String uuid) {
		DBCollection collection = mongoDatabase.getCollection(mongoCollectionName);
		JacksonDBCollection.wrap(collection, UUIDAssignment.class, String.class).insert(new UUIDAssignment(username, uuid));
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

	public class UUIDAssignment {

		private String uuid;

		private String username;

		public UUIDAssignment(String username, String uuid) {
			setUsername(username);
			setUuid(uuid);
		}

		public String getUuid() {
			return uuid;
		}

		public void setUuid(String uuid) {
			this.uuid = uuid;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}
	}
}