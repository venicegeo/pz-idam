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

import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.joda.time.DateTime;
import org.mongojack.DBQuery;
import org.mongojack.DBQuery.Query;
import org.mongojack.DBUpdate.Builder;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.venice.piazza.idam.model.ApiKey;
import org.venice.piazza.idam.model.user.UserThrottles;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoCredential;
import com.mongodb.MongoException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;

import model.logger.Severity;
import model.security.authz.UserProfile;
import util.PiazzaLogger;

@Component
public class MongoAccessor {
	@Value("${vcap.services.pz-mongodb.credentials.database}")
	private String DATABASE_NAME;
	@Value("${vcap.services.pz-mongodb.credentials.host}")
	private String DATABASE_HOST;
	@Value("${vcap.services.pz-mongodb.credentials.port}")
	private int DATABASE_PORT;
	@Value("${vcap.services.pz-mongodb.credentials.username:}")
	private String DATABASE_USERNAME;
	@Value("${vcap.services.pz-mongodb.credentials.password:}")
	private String DATABASE_CREDENTIAL;
	@Value("${mongo.db.collection.name}")
	private String API_KEY_COLLECTION_NAME;
	@Value("${mongo.db.userprofile.collection.name}")
	private String USER_PROFILE_COLLECTION_NAME;
	@Value("${mongo.db.throttle.collection.name}")
	private String THROTTLE_COLLECTION_NAME;
	@Value("${mongo.thread.multiplier}")
	private int mongoThreadMultiplier;
	@Value("${key.expiration.time.ms}")
	private Long KEY_EXPIRATION_DURATION_MS;
	@Value("${key.inactivity.threshold.ms}")
	private Long KEY_INACTIVITY_THESHOLD_MS;

	private static final Logger LOGGER = LoggerFactory.getLogger(MongoAccessor.class);
	private static final String USERNAME = "username";
	private static final String UUID = "uuid";
	private static final String INSTANCE_NOT_AVAILABLE_ERROR = "MongoDB instance not available.";

	@Autowired
	private PiazzaLogger pzLogger;
	@Autowired
	private Environment environment;

	private DB mongoDatabase;
	private MongoClient mongoClient;

	@PostConstruct
	private void initialize() {
		try {
			MongoClientOptions.Builder builder = new MongoClientOptions.Builder();
			// Enable SSL if the `mongossl` Profile is enabled
			if (Arrays.stream(environment.getActiveProfiles()).anyMatch(env -> env.equalsIgnoreCase("mongossl"))) {
				builder.sslEnabled(true);
				builder.sslInvalidHostNameAllowed(true);
			}
			// If a username and password are provided, then associate these credentials with the connection
			if ((!StringUtils.isEmpty(DATABASE_USERNAME)) && (!StringUtils.isEmpty(DATABASE_CREDENTIAL))) {
				mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, DATABASE_PORT),
						Arrays.asList(
								MongoCredential.createCredential(DATABASE_USERNAME, DATABASE_NAME, DATABASE_CREDENTIAL.toCharArray())),
						builder.threadsAllowedToBlockForConnectionMultiplier(mongoThreadMultiplier).build());
			} else {
				mongoClient = new MongoClient(new ServerAddress(DATABASE_HOST, DATABASE_PORT),
						builder.threadsAllowedToBlockForConnectionMultiplier(mongoThreadMultiplier).build());
			}
			mongoDatabase = mongoClient.getDB(DATABASE_NAME); // NOSONAR
		} catch (Exception exception) {
			LOGGER.error(String.format("Error connecting to MongoDB Instance. %s", exception.getMessage()), exception);

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
		// Create the new API Key Model
		ApiKey apiKey = new ApiKey(uuid, username, System.currentTimeMillis(), new DateTime().plus(KEY_EXPIRATION_DURATION_MS).getMillis());
		Query query = DBQuery.is("userName", username);
		// Update the old Key
		getApiKeyCollection().update(query, apiKey);
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
		ApiKey apiKey = new ApiKey(uuid, username, System.currentTimeMillis(), new DateTime().plus(KEY_EXPIRATION_DURATION_MS).getMillis());
		getApiKeyCollection().insert(apiKey);
	}

	/**
	 * Determines if an API Key is valid in the API Key Collection
	 * 
	 * @param uuid
	 *            The API Key
	 * @return True if valid. False if not.
	 */
	public boolean isApiKeyValid(String uuid) {
		Query query = DBQuery.is("apiKey", uuid);
		ApiKey apiKey = getApiKeyCollection().findOne(query);
		// Check that the key exists.
		if (apiKey == null) {
			// Key does not exist
			return false;
		}
		// Key exists. Check expiration date to ensure it's valid
		if (apiKey.getExpiresOn() < System.currentTimeMillis()) {
			// Key has expired and is not valid any longer
			return false;
		}
		// Key has not expired. Check Inactivity date.
		if ((System.currentTimeMillis() - apiKey.getLastUsedOn()) < KEY_INACTIVITY_THESHOLD_MS) {
			// Key is not inactive.
			// First, update the last time this key was used.
			try {
				Builder update = new Builder();
				update.set("lastUsedOn", System.currentTimeMillis());
				Query updateQuery = DBQuery.is("apiKey", uuid);
				getApiKeyCollection().update(updateQuery, update);
			} catch (Exception exception) {
				String error = "Could not update time of last usage for API Key.";
				LOGGER.error(error, exception);
				pzLogger.log(error, Severity.WARNING);
			}

			// Key is Valid
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets the current username for the API Key from the API Key Table
	 * 
	 * @param uuid
	 *            The user's API Key
	 * @return The username. Null if the API Key is not valid.
	 */
	public String getUsername(String uuid) {
		Query query = DBQuery.is("apiKey", uuid);
		ApiKey apiKey = getApiKeyCollection().findOne(query);
		if (apiKey != null) {
			return apiKey.getUserName();
		} else {
			return null;
		}
	}

	/**
	 * Gets the API Key for the specified username
	 * 
	 * @param username
	 *            The username
	 * @return The API Key. Null if no username has a matching API Key entry.
	 */
	public String getApiKey(String username) {
		Query query = DBQuery.is("userName", username);
		ApiKey apiKey = getApiKeyCollection().findOne(query);
		if (apiKey != null) {
			return apiKey.getApiKey();
		} else {
			return null;
		}
	}

	/**
	 * Gets the Mongo Collection for API Keys
	 * 
	 * @return API Key Collection
	 */
	public JacksonDBCollection<ApiKey, String> getApiKeyCollection() {
		DBCollection collection = mongoDatabase.getCollection(API_KEY_COLLECTION_NAME);
		return JacksonDBCollection.wrap(collection, ApiKey.class, String.class);
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
	 * Gets all User Throttles from the Database and returns an unsorted list of them.
	 * 
	 * @return List of all User Throttles.
	 */
	public List<UserThrottles> getAllUserThrottles() {
		return getUserThrottlesCollection().find().toArray();
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