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

import com.google.common.base.Verify;
import exception.InvalidInputException;
import model.security.ApiKey;
import model.security.authz.Throttle;
import model.security.authz.UserProfile;
import model.security.authz.UserThrottles;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.*;
import org.omg.CORBA.DynAnyPackage.Invalid;
import org.springframework.test.util.ReflectionTestUtils;
import org.venice.piazza.common.hibernate.dao.ApiKeyDao;
import org.venice.piazza.common.hibernate.dao.UserProfileDao;
import org.venice.piazza.common.hibernate.dao.UserThrottlesDao;
import org.venice.piazza.common.hibernate.entity.ApiKeyEntity;
import org.venice.piazza.common.hibernate.entity.UserProfileEntity;
import org.venice.piazza.common.hibernate.entity.UserThrottlesEntity;
import org.venice.piazza.idam.data.DatabaseAccessor;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import util.PiazzaLogger;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class DatabaseAccessorTests {


	private ApiKey apiKey = new ApiKey("valid_uuid", "valid_username", 1000, 1000);
	private UserProfile userProfile = new UserProfile();
	private UserThrottles userThrottles = new UserThrottles();

    @Spy
    ApiKeyEntity apiKeyEntity = new ApiKeyEntity();
    @Spy
	UserProfileEntity userProfileEntity = new UserProfileEntity();
    @Spy
	UserThrottlesEntity userThrottlesEntity = new UserThrottlesEntity();

	@Mock
	private PiazzaLogger logger;
	@Mock
    private ApiKeyDao apiKeyDao;
	@Mock
	private UserProfileDao userProfileDao;
	@Mock
	private UserThrottlesDao userThrottlesDao;

	@Spy
	@InjectMocks
	private DatabaseAccessor accessor;

	/**
	 * Initialize mock objects.
	 */
	@Before
	public void setup() {
	    MockitoAnnotations.initMocks(this);

	    //These values are from the application.properties file.
	    ReflectionTestUtils.setField(this.accessor, "KEY_EXPIRATION_DURATION_MS", 31556952000L);
	    ReflectionTestUtils.setField(this.accessor, "KEY_INACTIVITY_THESHOLD_MS", 15778476000L);

	    this.userProfile.setUsername(apiKey.getUsername());
	    this.userProfile.setDistinguishedName("my_dn");

	    this.apiKeyEntity.setApiKey(this.apiKey);
	    this.userProfileEntity.setUserProfile(this.userProfile);
	    this.userThrottlesEntity.setUserThrottles(this.userThrottles);

	    when(this.apiKeyDao.getApiKeyByUserName(apiKey.getUsername())).thenReturn(this.apiKeyEntity);
	    when(this.apiKeyDao.getApiKeyByUuid(apiKey.getUuid())).thenReturn(this.apiKeyEntity);

	    when(this.userProfileDao.getUserProfileByUserName(this.userProfile.getUsername())).thenReturn(this.userProfileEntity);
	    when(this.userProfileDao.getUserProfileByUserNameAndDn(this.userProfile.getUsername(), this.userProfile.getDistinguishedName()))
				.thenReturn(this.userProfileEntity);
	}

	@Test
	public void testUpdateApiKey() {
		accessor.updateApiKey("not_a_valid_username", "my_uuid");
        Mockito.verify(this.apiKeyDao, times(0)).save(any(ApiKeyEntity.class));

        accessor.updateApiKey("valid_username", "my_uuid");
        Mockito.verify(this.apiKeyDao, times(1)).save(any(ApiKeyEntity.class));
	}

	@Test
	public void testCreateApiKey() {
		accessor.createApiKey("valid_username", "my_uuid");
		Mockito.verify(this.apiKeyDao, times(1)).save(any(ApiKeyEntity.class));
	}

	@Test
	public void testIsApiKeyValid() {
		Assert.assertFalse(this.accessor.isApiKeyValid("invalid_uuid"));

		//Check expired key
		apiKey.setExpiresOn(System.currentTimeMillis() - (1000 * 60));
		Assert.assertFalse(this.accessor.isApiKeyValid("valid_uuid"));

		//Check inactive key.
		apiKey.setLastUsedOn(1000);
		apiKey.setExpiresOn(System.currentTimeMillis() + (1000 * 60));
		Assert.assertFalse(this.accessor.isApiKeyValid("valid_uuid"));

		//Check valid key.
		apiKey.setLastUsedOn(System.currentTimeMillis() - 1000);
		apiKey.setExpiresOn(System.currentTimeMillis() + (1000 * 60));
		Assert.assertTrue(this.accessor.isApiKeyValid("valid_uuid"));

		//Check unitialized/legacy key
		apiKey.setExpiresOn(0);
		apiKey.setLastUsedOn(0);
		Assert.assertTrue(this.accessor.isApiKeyValid("valid_uuid"));

		//Check exception.
		when(this.apiKeyDao.save(this.apiKeyEntity)).thenThrow(new RuntimeException("Dummy save exception."));
		Assert.assertTrue(this.accessor.isApiKeyValid("valid_uuid"));
	}

	@Test
	public void testGetUsername() {
		//Test a valid entity
		Assert.assertEquals(this.apiKey.getUsername(), this.accessor.getUsername(this.apiKey.getUuid()));

		//Test an invalid entity
		Assert.assertNull(this.accessor.getUsername("invalid_key"));
	}

	@Test
	public void testGetApiKey() {
		//Test a valid entity.
		Assert.assertEquals(this.apiKey.getUuid(), this.accessor.getApiKey(this.apiKey.getUsername()));

		//Test an invalid entity.
		Assert.assertNull(this.accessor.getApiKey("invalid_username"));
	}

	@Test
	public void testDeleteApiKey() throws InvalidInputException {
		this.accessor.deleteApiKey(this.apiKey.getUuid());
		Mockito.verify(this.apiKeyDao, times(1)).delete(this.apiKeyEntity);

		//Test a missing uuid.
		this.accessor.deleteApiKey("some_invalid_key");
		Mockito.verify(this.apiKeyDao, times(1)).delete(this.apiKeyEntity);
	}

	@Test(expected = InvalidInputException.class)
	public void testDeleteApiKeyException() throws InvalidInputException {
		this.accessor.deleteApiKey(null);
	}

	@Test
	public void testGetUserProfileByUsername() {
		//Test valid entity.
		Assert.assertEquals(this.userProfile, this.accessor.getUserProfileByUsername(this.userProfile.getUsername()));

		//Test a missing username.
		Assert.assertEquals("a_new_username", this.accessor.getUserProfileByUsername("a_new_username").getUsername());
	}

	@Test
	public void testGetUserProfiles() {
		Mockito.when(this.userProfileDao.findAll()).thenReturn(Collections.singleton(this.userProfileEntity));

		List<UserProfile> results = this.accessor.getUserProfiles();
		Assert.assertArrayEquals(new UserProfile[]{this.userProfile}, results.toArray());
	}

	@Test
	public void testGetUserProfilebyApiKey() {
		//Test missing key
		Assert.assertNull(this.accessor.getUserProfileByApiKey("an_invalid_key"));

		Assert.assertEquals(this.userProfile, this.accessor.getUserProfileByApiKey(this.apiKey.getUuid()));
	}

	@Test
	public void testUpdateUserProfile() {
		String newUsername = "my_new_username";
		String newDistinguishedName = "my_new_dn";
		UserProfile newProfile = new UserProfile();
		newProfile.setUsername(newUsername);
		newProfile.setDistinguishedName(newDistinguishedName);

		Assert.assertNotEquals(newProfile, this.userProfileEntity.getUserProfile());
		this.accessor.updateUserProfile(
				this.userProfileEntity.getUserProfile().getUsername(),
				this.userProfileEntity.getUserProfile().getDistinguishedName(),
				newProfile);

		Assert.assertEquals(newProfile, this.userProfileEntity.getUserProfile());
		Mockito.verify(this.userProfileDao, times(1)).save(this.userProfileEntity);
	}

	@Test
	public void testHasUserProfile() {
		Assert.assertTrue(this.accessor.hasUserProfile(this.userProfile.getUsername(), this.userProfile.getDistinguishedName()));
		Assert.assertFalse(this.accessor.hasUserProfile(this.userProfile.getUsername(), "invalid_dn"));
		Assert.assertFalse(this.accessor.hasUserProfile("invalid_username", this.userProfile.getDistinguishedName()));
	}

	@Test
	public void testInsertUserProfile() {
		Mockito.verify(this.userProfileDao, times(0)).save(any(UserProfileEntity.class));
		this.accessor.insertUserProfile(new UserProfile());
		Mockito.verify(this.userProfileDao, times(1)).save(any(UserProfileEntity.class));
	}

	@Test
	public void testDeleteUserProfile() throws InvalidInputException {
		Mockito.verify(this.userProfileDao, times(0)).delete(this.userProfileEntity);
		this.accessor.deleteUserProfile(this.userProfileEntity.getUserProfile().getUsername());
		Mockito.verify(this.userProfileDao, times(1)).delete(this.userProfileEntity);
	}

	@Test(expected = InvalidInputException.class)
	public void testDeleteUserProfileException() throws InvalidInputException {
		this.accessor.deleteUserProfile(null);
	}

	@Test
	public void testGetAllUserThrottles() {
		when(this.userThrottlesDao.findAll()).thenReturn(Collections.singleton(this.userThrottlesEntity));

		Assert.assertArrayEquals(new UserThrottles[]{this.userThrottles}, this.accessor.getAllUserThrottles().toArray());
	}

	@Test
	public void testGetCurrentThrottlesForUser() {
		String userName = this.userProfile.getUsername();
		Mockito.when(this.userThrottlesDao.getUserThrottlesByUserName(this.userProfile.getUsername())).thenReturn(this.userThrottlesEntity);

		Assert.assertEquals(this.userThrottlesEntity.getUserThrottles(), this.accessor.getCurrentThrottlesForUser(this.userProfileEntity.getUserProfile().getUsername(), false));

		Assert.assertNull(this.accessor.getCurrentThrottlesForUser("invalid_user_name", false));

		Assert.assertEquals("username2", this.accessor.getCurrentThrottlesForUser("username2", true).getUsername());

	}

	@Test
	public void testInsertUserThrottles() {

		Mockito.verify(this.userThrottlesDao, times(0)).save(any(UserThrottlesEntity.class));
		this.accessor.insertUserThrottles(new UserThrottles());
		Mockito.verify(this.userThrottlesDao, times(1)).save(any(UserThrottlesEntity.class));
	}

	@Test
	public void testIncrementUserThrottles() {
		Mockito.when(this.userThrottlesDao.getUserThrottlesByUserName(this.userProfile.getUsername())).thenReturn(this.userThrottlesEntity);
		int startValue = this.accessor.getInvocationsForUserThrottle(this.userProfile.getUsername(), Throttle.Component.QUERY);
		this.accessor.incrementUserThrottles(this.userProfileEntity.getUserProfile().getUsername(),
				Throttle.Component.QUERY);
		int endValue = this.accessor.getInvocationsForUserThrottle(this.userProfile.getUsername(), Throttle.Component.QUERY);
		Assert.assertEquals(startValue + 1, endValue);
	}

	@Test
	public void testClearThrottles() {
		this.accessor.clearThrottles();
		Mockito.verify(this.userThrottlesDao, times(1)).deleteAll();
	}



}