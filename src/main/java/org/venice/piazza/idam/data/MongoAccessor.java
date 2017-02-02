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

import org.joda.time.DateTime;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.DBUpdate.Builder;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.model.user.UserThrottles;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;

import model.logger.Severity;
import model.security.authz.UserProfile;
import model.service.metadata.Service;
import util.PiazzaLogger;

@Component
public class MongoAccessor {
	@Value("${vcap.services.pz-mongodb.credentials.uri}")
	private String mongoHost;
	@Value("${vcap.services.pz-mongodb.credentials.database}")
	private String mongoDBName;
	@Value("${mongo.db.collection.name}")
	private String API_KEY_COLLECTION_NAME;
	@Value("${mongo.db.userprofile.collection.name}")
	private String USER_PROFILE_COLLECTION_NAME;
	@Value("${mongo.db.throttle.collection.name}")
	private String THROTTLE_COLLECTION_NAME;
	@Value("${mongo.thread.multiplier}")
	private int mongoThreadMultiplier;

	private static final Logger LOGGER = LoggerFactory.getLogger(MongoAccessor.class);
	private static final String USERNAME = "username";
	private static final String UUID = "uuid";
	private static final String INSTANCE_NOT_AVAILABLE_ERROR = "MongoDB instance not available.";

	@Autowired
	private PiazzaLogger pzLogger;

	private DB mongoDatabase;
	private MongoClient mongoClient;

	@PostConstruct
	private void initialize() {
		try {
			MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
			mongoClient = new MongoClient(
					new MongoClientURI(mongoHost, builder.threadsAllowedToBlockForConnectionMultiplier(mongoThreadMultiplier)));
			mongoDatabase = mongoClient.getDB(mongoDBName); // NOSONAR
		} catch (Exception exception) {
			String error = String.format("Error Contacting Mongo Host %s: %s", mongoHost, exception.getMessage());
			LOGGER.error(error, exception);
			pzLogger.log(error, Severity.ERROR);
		}
	}

	@PreDestroy
	private void close() {
		mongoClient.close();
	}

	/**
	 * Updates the API Key for the specified user in DB
	 * 
	 * @param username
	 *            The username
	 * @param uuid
	 *            The updated API Key
	 */
	public void updateApiKey(String username, String uuid) {
		BasicDBObject newObj = new BasicDBObject();
		newObj.put(USERNAME, username);
		newObj.put(UUID, uuid);
		mongoDatabase.getCollection(API_KEY_COLLECTION_NAME).update(new BasicDBObject().append(USERNAME, username), newObj);
	}

	/**
	 * Creates the API Key for the specified User in DB
	 * 
	 * @param username
	 *            The username
	 * @param uuid
	 *            The API Key for the user name
	 */
	public void createApiKey(String username, String uuid) {
		mongoDatabase.getCollection(API_KEY_COLLECTION_NAME).insert(new BasicDBObject().append(USERNAME, username).append(UUID, uuid));
	}

	/**
	 * Determines if an API Key is valid in the UUID Collection
	 * 
	 * @param uuid
	 *            The API Key
	 * @return True if valid. False if not.
	 */
	public boolean isApiKeyValid(String uuid) {
		DBObject obj = mongoDatabase.getCollection(API_KEY_COLLECTION_NAME).findOne(new BasicDBObject(UUID, uuid));
		if (obj != null) {
			return true;
		}
		return false;
	}

	/**
	 * Gets the current username for the API Key from the API Key Table
	 * 
	 * @param uuid
	 *            The user's API Key
	 * @return The username
	 */
	public String getUsername(String uuid) {
		DBObject obj = mongoDatabase.getCollection(API_KEY_COLLECTION_NAME).findOne(new BasicDBObject(UUID, uuid));
		if (obj != null && obj.containsField(USERNAME)) {
			return obj.get(USERNAME).toString();
		}
		return null;
	}

	/**
	 * Gets the API Key for the specified username
	 * 
	 * @param username
	 *            The username
	 * @return The API Key
	 */
	public String getApiKey(String username) {
		DBObject obj = mongoDatabase.getCollection(API_KEY_COLLECTION_NAME).findOne(new BasicDBObject(USERNAME, username));
		if (obj != null && obj.containsField(UUID)) {
			return obj.get(UUID).toString();
		}
		return null;
	}

	/**
	 * Gets the Profile for the specified user
	 * 
	 * @param username
	 *            The username
	 * @return The User Profile
	 */
	public UserProfile getUserProfileByUsername(String username) {
		BasicDBObject query = new BasicDBObject(USERNAME, username);
		UserProfile userProfile;

		try {
			if ((userProfile = getUserProfileCollection().findOne(query)) == null) {
				// TODO: Current hack, until we commit the UserProfile to Mongo during /key creation.
				// TODO: In a clean environment (with a freshly deleted UUID table) this code can be removed. It's
				// remaining for legacy where Keys might exist w/o UserProfiles.
				userProfile = new UserProfile();
				userProfile.setUsername(username);
				return userProfile;
			}
		} catch (MongoTimeoutException mte) {
			LOGGER.error(INSTANCE_NOT_AVAILABLE_ERROR, mte);
			throw new MongoException(INSTANCE_NOT_AVAILABLE_ERROR);
		}

		return userProfile;
	}

	/**
	 * Gets the Profile for the specified user with a valid API Key
	 * 
	 * @param uuid
	 *            The API Key
	 * @return The User Profile.
	 */
	public UserProfile getUserProfileByApiKey(String uuid) {
		String username = getUsername(uuid);
		if (username == null) {
			return null;
		} else {
			return getUserProfileByUsername(username);
		}
	}

	/**
	 * Gets the Mongo Collection of all User Profiles
	 * 
	 * @return Mongo collection for Profiles
	 */
	private JacksonDBCollection<UserProfile, String> getUserProfileCollection() {
		DBCollection collection = mongoDatabase.getCollection(USER_PROFILE_COLLECTION_NAME);
		return JacksonDBCollection.wrap(collection, UserProfile.class, String.class);
	}

	/**
	 * Determines if the current Username (with matching Distinguished Name) has a UserProfile in the Piazza database.
	 * 
	 * @param username
	 *            The username
	 * @param dn
	 *            The distinguished name
	 * @return True if the User has a UserProfile in the DB, false if not.
	 */
	public boolean hasUserProfile(String username, String dn) {
		Query query = DBQuery.empty();
		query.and(DBQuery.is("username", username));
		query.and(DBQuery.is("distinguishedName", dn));
		UserProfile userProfile = getUserProfileCollection().findOne(query);
		if (userProfile == null) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Inserts a new User Profile into the database. Auto-populates things such as creation date to the current
	 * timestamp.
	 * <p>
	 * The action of creating a User Profile is performed when the user first generates their API Key for Piazza. The
	 * User Profile contains static metadata information (not the API Key) related to that user.
	 * </p>
	 * 
	 * @param username
	 *            The name of the user.
	 * @param dn
	 *            The distinguished name of the user
	 */
	public UserProfile insertUserProfile(String username, String dn) {
		// Create Model
		UserProfile userProfile = new UserProfile();
		userProfile.setUsername(username);
		userProfile.setDistinguishedName(dn);
		userProfile.setCreatedOn(new DateTime());
		// Commit
		getUserProfileCollection().insert(userProfile);
		// Return
		return userProfile;
	}

	/**
	 * Gets the Mongo Collection of the User Throttles. This is the information that determines how many invocations of
	 * a particular action that a user has performed in the last period of time.
	 * 
	 * @return Mongo collection for User Throttles
	 */
	private JacksonDBCollection<UserThrottles, String> getUserThrottlesCollection() {
		DBCollection collection = mongoDatabase.getCollection(THROTTLE_COLLECTION_NAME);
		return JacksonDBCollection.wrap(collection, UserThrottles.class, String.class);
	}

	/**
	 * Returns the current Throttles information for the specified user.
	 * 
	 * @param username
	 *            The user
	 * @param createIfNull
	 *            If true, the user throttle information will be added to the database if it doesn't exist already.
	 * @return The throttle information, containing counts for invocations of each Throttle
	 */
	public UserThrottles getCurrentThrottlesForUser(String username, boolean createIfNull) throws MongoException {
		BasicDBObject query = new BasicDBObject("username", username);
		UserThrottles userThrottles;

		try {
			userThrottles = getUserThrottlesCollection().findOne(query);
		} catch (MongoTimeoutException mte) {
			LOGGER.error(INSTANCE_NOT_AVAILABLE_ERROR, mte);
			throw new MongoException(INSTANCE_NOT_AVAILABLE_ERROR);
		}

		// If the User Throttles object doesn't exist, then create a default one and set the initial values.
		if ((userThrottles == null) && (createIfNull)) {
			// Get the set of default throttles
			userThrottles = new UserThrottles(username);
			// Commit to the database
			insertUserThrottles(userThrottles);
		}

		return userThrottles;
	}

	/**
	 * Gets the current number of invocations for the specified user for the specified component
	 * 
	 * @param username
	 *            The username
	 * @param component
	 *            The component
	 * @return The number of invocations
	 */
	public Integer getInvocationsForUserThrottle(String username, model.security.authz.Throttle.Component component) throws MongoException {
		return getCurrentThrottlesForUser(username, true).getThrottles().get(component.toString());
	}

	/**
	 * Adds an entry for user throttles in the database.
	 * 
	 * @param userThrottles
	 *            The Throttles object, containing the username.
	 */
	public void insertUserThrottles(UserThrottles userThrottles) throws MongoException {
		getUserThrottlesCollection().insert(userThrottles);
	}

	/**
	 * Increments the count for a users throttles for the specific component.
	 * 
	 * @param component
	 *            The component, as defined in the Throttle model
	 */
	public void incrementUserThrottles(String username, model.security.authz.Throttle.Component component) throws MongoException {
		Integer currentInvocations = getInvocationsForUserThrottle(username, component);
		Builder update = new Builder();
		update.set(String.format("throttles.%s", component.toString()), ++currentInvocations);
		Query query = DBQuery.is("username", username);
		getUserThrottlesCollection().update(query, update);
	}

	/**
	 * Clears all throttle invocations in the Throttle table.
	 */
	public void clearThrottles() {
		getUserThrottlesCollection().drop();
	}
}