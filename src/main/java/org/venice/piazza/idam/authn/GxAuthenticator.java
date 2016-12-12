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

		logger.log(String.format("GeoAxis response for Username %s has returned Authenticated %s", username, gxResponse.isSuccessful()),
				Severity.INFORMATIONAL,
				new AuditElement(username, gxResponse.isSuccessful() ? "userLoggedIn" : "userFailedAuthentication", ""));

		return new AuthResponse(gxResponse.isSuccessful(), mongoAccessor.getUserProfileByUsername(username));
	}

	@Override
	public AuthResponse getAuthenticationDecision(String pem) {
		logger.log(String.format("Performing cert check for PKI Cert to GeoAxis"), Severity.INFORMATIONAL,
				new AuditElement("idam", "loginCertAttempt", ""));

		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(getFormattedPem(pem));
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");

		GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnCert, request, GxAuthNResponse.class);

		if (gxResponse.isSuccessful() == false) {
			logger.log(String.format("GeoAxis response for PKI Cert has failed authentication."), Severity.INFORMATIONAL,
					new AuditElement("idam", "userFailedCertAuthentication", ""));
		}

		if (gxResponse.getPrincipals() != null && gxResponse.getPrincipals().getPrincipal() != null) {
			List<PrincipalItem> listItems = gxResponse.getPrincipals().getPrincipal();
			for (PrincipalItem item : listItems) {
				if ("UID".equalsIgnoreCase(item.getName())) {
					UserProfile profile = mongoAccessor.getUserProfileByUsername(item.getValue());
					logger.log(String.format("GeoAxis response for PKI Cert has passed authentication, Username %s", profile.getUsername()),
							Severity.INFORMATIONAL);
					return new AuthResponse(gxResponse.isSuccessful(), profile);
				}
			}
		}
		return new AuthResponse(false);
	}

	private String getFormattedPem(String pem) {
		String pemHeader = "-----BEGIN CERTIFICATE-----";
		String pemFooter = "-----END CERTIFICATE-----";
		String pemInternal = pem.substring(pemHeader.length(), pem.length() - pemFooter.length() - 1);

		return pemHeader + "\n" + pemInternal.trim().replaceAll(" +", "\n") + "\n" + pemFooter;
	}
}