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
package org.venice.piazza.idam.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.model.GxAuthARequest;
import org.venice.piazza.idam.model.GxAuthAResponse;

import model.logger.AuditElement;
import model.logger.Severity;
import model.security.authz.UserProfile;
import util.PiazzaLogger;

@Component
@Profile({ "geoaxis" })
public class GxUserProfileClient {

	@Value("${vcap.services.geoaxis.credentials.api.url.ata}")
	private String gxApiUrlAta;	

	@Autowired
	private PiazzaLogger logger;

	@Autowired
	private RestTemplate restTemplate;
	
	public UserProfile getUserProfileFromGx(final String username)  {
		logger.log("Attempting to retrieve user profile attributes from GeoAxis", Severity.INFORMATIONAL,
				new AuditElement("idam", "profileAttributeRetrievalAttempt", ""));

		final GxAuthARequest request = new GxAuthARequest();
		request.setUid(username);

		final GxAuthAResponse[] gxResponse = restTemplate.postForObject(gxApiUrlAta, request, GxAuthAResponse[].class);
		
		logger.log("GeoAxis response for user profile attributes successful", Severity.INFORMATIONAL,
				new AuditElement("idam", "profileAttributeRetrieved", ""));

		final UserProfile userProfile = new UserProfile();

		if( gxResponse != null && gxResponse.length > 0 ) {
			final GxAuthAResponse firstElement = gxResponse[0];

			if( firstElement.getNationalityextended() != null && !firstElement.getNationalityextended().isEmpty()) { 
				userProfile.setCountry(firstElement.getNationalityextended().get(0));
			}
			
			/*
			 * If NGA:
			 *    admincode = serviceoragency
			 *    dutycode = serviceoragency
			 *    
			 * If non-NGA:
			 * 	  admincode = gxadministrativeorganizationcode
			 *    dutycode = gxdutydodoccupationcode
			 */	
			
			if( firstElement.getServiceoragency() != null && !firstElement.getServiceoragency().isEmpty()) {
				final String serviceOrAgencyValue = firstElement.getServiceoragency().get(0);
				
				if( serviceOrAgencyValue.equalsIgnoreCase("NGA") ) {
					userProfile.setAdminCode(firstElement.getServiceoragency().get(0));
					userProfile.setDutyCode(firstElement.getServiceoragency().get(0));
				}
				else {
					if( firstElement.getGxadministrativeorganizationcode() != null && !firstElement.getGxadministrativeorganizationcode().isEmpty()) {
						userProfile.setAdminCode(firstElement.getGxadministrativeorganizationcode().get(0));
					}
					
					if( firstElement.getGxdutydodoccupationcode() != null && !firstElement.getGxdutydodoccupationcode().isEmpty()) {
						userProfile.setDutyCode(firstElement.getGxdutydodoccupationcode().get(0));
					}
				}
			}
		}
		
		return userProfile;
	}
}
