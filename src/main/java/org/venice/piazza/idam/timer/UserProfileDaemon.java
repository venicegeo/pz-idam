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
package org.venice.piazza.idam.timer;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.venice.piazza.idam.data.DatabaseAccessor;
import org.venice.piazza.idam.util.GxUserProfileClient;

import exception.InvalidInputException;
import model.logger.AuditElement;
import model.logger.Severity;
import model.security.authz.UserProfile;
import util.PiazzaLogger;

@Component
@Profile({ "geoaxis" })
public class UserProfileDaemon {

	@Value("${vcap.services.geoaxis.credentials.api.url.ata}")
	private String gxApiUrlAta;
	@Autowired
	private PiazzaLogger logger;
	@Autowired
	private DatabaseAccessor accessor;
	@Autowired
	private GxUserProfileClient gxUserProfileClient;
	
	@Scheduled(cron = "0 0 0 * * SUN")
	private void verifyExistingApiKeys() throws InvalidInputException {
	
		logger.log("UserProfileDaemon starting to check existing UserProfiles for validity!", Severity.INFORMATIONAL,
				new AuditElement("idam", "profileAttributeRetrievalAttemptDAEMON", ""));
		
		final List<UserProfile> userProfiles = accessor.getUserProfiles();
		
		for( final UserProfile originalUserProfile : userProfiles ) {
			final String username = originalUserProfile.getUsername();
			final String dn = originalUserProfile.getDistinguishedName();
			final UserProfile newUserProfile = gxUserProfileClient.getUserProfileFromGx(username, dn);
			
			if( !isUserProfileActive(newUserProfile) ) {
				// Profile not active, remove ApiKey and UserProfile
				logger.log("UserProfileDaemon failed to verify UserProfile for user: " + username, Severity.INFORMATIONAL,
						new AuditElement("idam", "userProfileVerificationFailureDAEMON", ""));				
				
				final String apiKey = accessor.getApiKey(username);
				accessor.deleteApiKey(apiKey);
				accessor.deleteUserProfile(username);
			}
			else {
				// Log verified ApiKey
				logger.log("UserProfileDaemon successfully verified UserProfile for user: " + username, Severity.INFORMATIONAL,
						new AuditElement("idam", "userProfileVerificationSuccessDAEMON", ""));				
			}
		}		
	}
	
	private boolean isUserProfileActive(final UserProfile userProfile) {
		boolean isCountryPopulated = false;
		boolean isAdminCodePopulated = false;
		boolean isDutyCodePopulated = false;
		
		if( userProfile.getCountry() != null && !userProfile.getCountry().isEmpty()) {
			isCountryPopulated = true;
		}
		if( userProfile.getAdminCode() != null && !userProfile.getAdminCode().isEmpty()) {
			isAdminCodePopulated = true;
		}
		if( userProfile.getDutyCode() != null && !userProfile.getDutyCode().isEmpty()) {
			isDutyCodePopulated = true;
		}
		
		return isCountryPopulated && isAdminCodePopulated && isDutyCodePopulated;
	}
}
