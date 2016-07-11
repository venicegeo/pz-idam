package org.venice.piazza.security.data;

import javax.annotation.PostConstruct;

import org.mongojack.JacksonDBCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.mongodb.DBCollection;
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
	
	private MongoClient mongoClient;
	
	@PostConstruct
	private void initialize() {
		try {
			mongoClient = new MongoClient(new MongoClientURI(mongoHost));
		} catch (Exception ex) {
			logger.log(String.format("Error Contacting Mongo Host %s: %s", mongoHost, ex.getMessage()), PiazzaLogger.ERROR);
		}
	}
	
	public void save(String username, String uuid) {
		DBCollection collection = mongoClient.getDB(mongoDBName).getCollection(mongoCollectionName);
		
		JacksonDBCollection<UUIDAssignment,String> coll = JacksonDBCollection.wrap(collection, UUIDAssignment.class, String.class);
		
		coll.insert(new UUIDAssignment(username, uuid));
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