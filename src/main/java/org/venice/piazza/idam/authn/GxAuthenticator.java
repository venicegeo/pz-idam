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
package org.venice.piazza.idam.authn;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.GxAuthARequest;
import org.venice.piazza.idam.model.GxAuthAResponse;
import org.venice.piazza.idam.model.GxAuthNCertificateRequest;
import org.venice.piazza.idam.model.GxAuthNResponse;
import org.venice.piazza.idam.model.GxAuthNUserPassRequest;
import org.venice.piazza.idam.model.PrincipalItem;

import model.logger.AuditElement;
import model.logger.Severity;
import model.response.AuthResponse;
import model.security.authz.UserProfile;
import util.PiazzaLogger;

/**
 * AuthN requests to GeoAxis to verify user identity via user name/password or PKI cert.
 * 
 * @author Russel.Orf
 *
 */
@Component
@Profile({ "geoaxis" })
public class GxAuthenticator implements PiazzaAuthenticator {

	@Value("${vcap.services.geoaxis.credentials.api.url.atncert}")
	private String gxApiUrlAtnCert;
	@Value("${vcap.services.geoaxis.credentials.api.url.atnbasic}")
	private String gxApiUrlAtnBasic;
	@Value("${vcap.services.geoaxis.credentials.api.url.ata}")
	private String gxApiUrlAta;	
	@Value("${vcap.services.geoaxis.credentials.basic.mechanism}")
	private String gxBasicMechanism;
	@Value("${vcap.services.geoaxis.credentials.basic.hostidentifier}")
	private String gxBasicHostIdentifier;
	@Autowired
	private PiazzaLogger logger;

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private MongoAccessor mongoAccessor;

	@Override
	public AuthResponse getAuthenticationDecision(final String username, final String credential) {
		logger.log(String.format("Performing credential check for Username %s to GeoAxis.", username), Severity.INFORMATIONAL,
				new AuditElement(username, "loginAttempt", ""));
		final GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername(username);
		request.setPassword(credential);
		request.setMechanism(gxBasicMechanism);
		request.setHostIdentifier(gxBasicHostIdentifier);

		final GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnBasic, request, GxAuthNResponse.class);

		return processGxResponse(gxResponse);
	}

	@Override
	public AuthResponse getAuthenticationDecision(final String pem) {
		logger.log("Performing cert check for PKI Cert to GeoAxis", Severity.INFORMATIONAL,
				new AuditElement("idam", "loginCertAttempt", ""));
		
		final GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(getFormattedPem(pem));
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");

		final GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnCert, request, GxAuthNResponse.class);

		return processGxResponse(gxResponse);
	}
	
	private AuthResponse processGxResponse(final GxAuthNResponse gxResponse) {
		boolean isAuthenticated = false;
		UserProfile userProfile = null;
		
		if( gxResponse != null ) {
			logger.log(String.format("GeoAxis response has returned authentication %s", gxResponse.isSuccessful()),
					Severity.INFORMATIONAL,
					new AuditElement("idam", gxResponse.isSuccessful() ? "userLoggedIn" : "userFailedAuthentication", ""));

			// If Authentication was successful, then get/create the User Profile.
			if( gxResponse.isSuccessful() ) {
				userProfile = getUserProfile(gxResponse);
	
//				if( userProfile != null && userProfile.isNPE() ) { // UNCOMMENT FOR NPE-ONLY ENFORCEMENT
					isAuthenticated = true;
//				}
			}
		} else {
			logger.log("GeoAxis has returned a NULL response!", Severity.INFORMATIONAL,
					new AuditElement("idam", "userFailedAuthentication", ""));
		}
		
		return new AuthResponse(isAuthenticated, userProfile);
	}

	/**
	 * Creates a User Profile in the Mongo DB, if one does not already exist. If it exists, this will return the
	 * existing UserProfile.
	 * 
	 * @param gxResponse
	 *            The GeoAxis response, containing at a minimum the username and DN
	 * @return The UserProfile object for this User
	 */
	private UserProfile getUserProfile(final GxAuthNResponse gxResponse) {
		// Extract the Username and DN from the Response
		String username = null;
		String dn = null;
		if (gxResponse.getPrincipals() != null && gxResponse.getPrincipals().getPrincipal() != null) {
			List<PrincipalItem> listItems = gxResponse.getPrincipals().getPrincipal();
			for (PrincipalItem item : listItems) {
				if ("UID".equalsIgnoreCase(item.getName())) {
					username = item.getValue();
				} else if ("DN".equalsIgnoreCase(item.getName())) {
					dn = item.getValue();
				}
			}
		}

		// Ensure that the username and dn both exist in the response
		if ((username != null) && (dn != null)) {
			// Log
			logger.log(String.format("GeoAxis response has passed authentication, Username %s with DN %s", username, dn),
					Severity.INFORMATIONAL);

			// Detect if the UserProfile exists
			UserProfile userProfile = null;
			if (mongoAccessor.hasUserProfile(username, dn)) {
				// Get the User Profile
				userProfile = mongoAccessor.getUserProfileByUsername(username);
			} 
			else {	
				Map<String,String> userProfileAttributes = getUserProfileAttributesFromGx(username);
				userProfile = mongoAccessor.insertUserProfile(username, dn, 
						userProfileAttributes.get("adminCode"), userProfileAttributes.get("dutyCode"), userProfileAttributes.get("country"));
			}

			// Return the Profile
			return userProfile;
		} else {
			// Log
			logger.log(String.format("GeoAxis Response was successful, but failed to get User Profile information for name %s and DN %s.",
					username, dn), Severity.ERROR);
			// Parse errors? Payload not present? This is an error. Don't attempt any further creation.
			return null;
		}
	}
	
	private String getFormattedPem(final String pem) {
		String pemHeader = "-----BEGIN CERTIFICATE-----";
		String pemFooter = "-----END CERTIFICATE-----";
		String pemInternal = pem.substring(pemHeader.length(), pem.length() - pemFooter.length() - 1);

		return pemHeader + "\n" + pemInternal.trim().replaceAll(" +", "\n") + "\n" + pemFooter;
	}
	
	private Map<String,String> getUserProfileAttributesFromGx(final String username)  {
		logger.log("Attempting to retrieve user profile attributes from GeoAxis", Severity.INFORMATIONAL,
				new AuditElement("idam", "profileAttributeRetrievalAttempt", ""));

		final GxAuthARequest request = new GxAuthARequest();
		request.setUid(username);

		final GxAuthAResponse[] gxResponse = restTemplate.postForObject(gxApiUrlAta, request, GxAuthAResponse[].class);
		
			logger.log("GeoAxis response for user profile attributes successful", Severity.INFORMATIONAL,
					new AuditElement("idam", "profileAttributeRetrieved", ""));

		final Map<String,String> userAttributes = new HashMap<>();

		if( gxResponse != null && gxResponse.length > 0 ) {
			final GxAuthAResponse firstElement = gxResponse[0];

			if( firstElement.getNationalityextended() != null && !firstElement.getNationalityextended().isEmpty()) { 
				userAttributes.put("country", firstElement.getNationalityextended().get(0));
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
					userAttributes.put("adminCode", firstElement.getServiceoragency().get(0));
					userAttributes.put("dutyCode", firstElement.getServiceoragency().get(0));
				}
				else {
					if( firstElement.getGxadministrativeorganizationcode() != null && !firstElement.getGxadministrativeorganizationcode().isEmpty()) {
						userAttributes.put("adminCode", firstElement.getGxadministrativeorganizationcode().get(0));
					}
					
					if( firstElement.getGxdutydodoccupationcode() != null && !firstElement.getGxdutydodoccupationcode().isEmpty()) {
						userAttributes.put("dutyCode", firstElement.getGxdutydodoccupationcode().get(0));
					}
				}
			}
		}
		
		return userAttributes;
	}
}