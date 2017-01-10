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

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.GxAuthNCertificateRequest;
import org.venice.piazza.idam.model.GxAuthNResponse;
import org.venice.piazza.idam.model.GxAuthNUserPassRequest;
import org.venice.piazza.idam.model.PrincipalItem;

import exception.InvalidInputException;
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
	public AuthResponse getAuthenticationDecision(String username, String credential) {
		logger.log(String.format("Performing credential check for Username %s to GeoAxis.", username), Severity.INFORMATIONAL,
				new AuditElement(username, "loginAttempt", ""));
		GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername(username);
		request.setPassword(credential);
		request.setMechanism(gxBasicMechanism);
		request.setHostIdentifier(gxBasicHostIdentifier);

		GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnBasic, request, GxAuthNResponse.class);

		// If Authenticated, then get/create the UserProfile for this user
		UserProfile userProfile = null;
		if (gxResponse.isSuccessful()) {
			userProfile = getUserProfile(gxResponse);
		}

		logger.log(String.format("GeoAxis response for Username %s has returned Authenticated %s", username, gxResponse.isSuccessful()),
				Severity.INFORMATIONAL,
				new AuditElement(username, gxResponse.isSuccessful() ? "userLoggedIn" : "userFailedAuthentication", ""));

		return new AuthResponse(gxResponse.isSuccessful(), userProfile);
	}

	@Override
	public AuthResponse getAuthenticationDecision(String pem) {
		logger.log("Performing cert check for PKI Cert to GeoAxis", Severity.INFORMATIONAL,
				new AuditElement("idam", "loginCertAttempt", ""));

		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(getFormattedPem(pem));
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");

		GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnCert, request, GxAuthNResponse.class);

		if (gxResponse.isSuccessful() == false) {
			logger.log("GeoAxis response for PKI Cert has failed authentication.", Severity.INFORMATIONAL,
					new AuditElement("idam", "userFailedCertAuthentication", ""));
		}

		// If Authentication was successful, then get/create the User Profile.
		UserProfile userProfile = null;
		if (gxResponse.isSuccessful()) {
			userProfile = getUserProfile(gxResponse);
		}

		// Return
		return new AuthResponse(gxResponse.isSuccessful(), userProfile);
	}

	/**
	 * Creates a User Profile in the Mongo DB, if one does not already exist. If it exists, this will return the
	 * existing UserProfile.
	 * 
	 * @param gxResponse
	 *            The GeoAxis response, containing at a minimum the username and DN
	 * @return The UserProfile object for this User
	 */
	private UserProfile getUserProfile(GxAuthNResponse gxResponse) {
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
			} else {
				// Create if it doesn't
				userProfile = mongoAccessor.insertUserProfile(username, dn);
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

	private String getFormattedPem(String pem) {
		String pemHeader = "-----BEGIN CERTIFICATE-----";
		String pemFooter = "-----END CERTIFICATE-----";
		String pemInternal = pem.substring(pemHeader.length(), pem.length() - pemFooter.length() - 1);

		return pemHeader + "\n" + pemInternal.trim().replaceAll(" +", "\n") + "\n" + pemFooter;
	}
}