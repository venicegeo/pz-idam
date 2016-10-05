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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.model.GxAuthNUserPassRequest;
import org.venice.piazza.idam.model.GxAuthNCertificateRequest;
import org.venice.piazza.idam.model.GxAuthNResponse;

@Component
@Profile({ "geoaxis" })
public class GxAuthenticator implements PiazzaAuthenticator {

	@Value("${vcap.services.geoaxis.api.url.atncert}")
	private String GX_API_URL_ATN_CERT;
	@Value("${vcap.services.geoaxis.api.url.atnbasic}")
	private String GX_API_URL_ATN_BASIC;

	@Autowired
	private RestTemplate restTemplate;

	public boolean getAuthenticationDecision(String username, String credential) {
		GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername(username);
		request.setPassword(credential);
		request.setMechanism("GxDisAus");
		request.setHostIdentifier("//OAMServlet/disaususerprotected");

		return restTemplate.postForObject(GX_API_URL_ATN_BASIC, request, GxAuthNResponse.class).isSuccessful();
	}

	public boolean getAuthenticationDecision(String pem) {
		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(pem);
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");

		return restTemplate.postForObject(GX_API_URL_ATN_CERT, request, GxAuthNResponse.class).isSuccessful();
	}
}