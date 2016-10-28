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
import org.venice.piazza.idam.model.GxAuthNUserPassRequest;
import org.venice.piazza.idam.model.PrincipalItem;

import model.response.AuthenticationResponse;

import org.venice.piazza.idam.data.MongoAccessor;
import org.venice.piazza.idam.model.GxAuthNCertificateRequest;
import org.venice.piazza.idam.model.GxAuthNResponse;

@Component
@Profile({ "geoaxis" })
public class GxAuthenticator implements PiazzaAuthenticator {

	@Value("${vcap.services.geoaxis.credentials.api.url.atncert}")
	private String gxApiUrlAtnCert;
	@Value("${vcap.services.geoaxis.credentials.api.url.atnbasic}")
	private String gxApiUrlAtnBasic;

	@Autowired
	private RestTemplate restTemplate;
	@Autowired
	private MongoAccessor mongoAccessor;

	@Override
	public AuthenticationResponse getAuthenticationDecision(String username, String credential) {
		GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername(username);
		request.setPassword(credential);
		request.setMechanism("GxDisAus");
		request.setHostIdentifier("//OAMServlet/disaususerprotected");

		GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnBasic, request, GxAuthNResponse.class);

		return new AuthenticationResponse(mongoAccessor.getUserProfileByUsername(username), gxResponse.isSuccessful());
	}

	@Override
	public AuthenticationResponse getAuthenticationDecision(String pem) {
		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(getFormattedPem(pem));
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");

		GxAuthNResponse gxResponse = restTemplate.postForObject(gxApiUrlAtnCert, request, GxAuthNResponse.class);

		if (gxResponse.getPrincipals() != null && gxResponse.getPrincipals().getPrincipal() != null) {
			List<PrincipalItem> listItems = gxResponse.getPrincipals().getPrincipal();
			for (PrincipalItem item : listItems) {
				if ("UID".equalsIgnoreCase(item.getName())) {
					return new AuthenticationResponse(mongoAccessor.getUserProfileByUsername(item.getValue()), gxResponse.isSuccessful());
				}
			}
		}
		return new AuthenticationResponse(null, false);
	}

	private String getFormattedPem(String pem) {
		String pemHeader = "-----BEGIN CERTIFICATE-----";
		String pemFooter = "-----END CERTIFICATE-----";
		String pemInternal = pem.substring(pemHeader.length(), pem.length() - pemFooter.length() - 1);

		return pemHeader + "\n" + pemInternal.trim().replaceAll(" +", "\n") + "\n" + pemFooter;
	}
}