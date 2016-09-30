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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.model.GxAuthNUserPassRequest;
import org.venice.piazza.idam.model.GxAuthNCertificateRequest;
import org.venice.piazza.idam.model.GxAuthNResponse;

@Component
@Profile( { "geoaxis" })
public class GEOAxISAuthenticator implements PiazzaAuthenticator {

	@Value("${vcap.services.geoaxis.api.url}")
	private String GX_API_URL;
	@Value("${vcap.services.geoaxis.api.uri.atncert}")
	private String GX_API_URI_ATNCERT;
	@Value("${vcap.services.geoaxis.api.uri.atnbasic}")
	private String GX_API_URI_ATNBASIC;
	
	private final String GX_API_URL_ATN_BASIC = GX_API_URL + GX_API_URI_ATNBASIC;
	private final String GX_API_URL_ATN_CERT = GX_API_URL + GX_API_URI_ATNCERT;

	@Autowired
	private RestTemplate restTemplate;
	
	public boolean getAuthenticationDecision(String username, String credential, String mechanism) {

		GxAuthNUserPassRequest request = new GxAuthNUserPassRequest();
		request.setUsername(username);
		request.setPassword(credential);
		request.setMechanism(mechanism);
		request.setHostIdentifier("//OAMServlet/basicprotected");
		
		ResponseEntity<GxAuthNResponse> response = restTemplate.postForEntity(GX_API_URL_ATN_BASIC, request, GxAuthNResponse.class);
		
		return response.getBody().isSuccessful();
	}
	
	public boolean getAuthenticationDecision(String pem) {

		GxAuthNCertificateRequest request = new GxAuthNCertificateRequest();
		request.setPemCert(pem);
		request.setMechanism("GxCert");
		request.setHostIdentifier("//OAMServlet/certprotected");
		
		ResponseEntity<GxAuthNResponse> response = restTemplate.postForEntity(GX_API_URL_ATN_CERT, request, GxAuthNResponse.class);
				
		return response.getBody().isSuccessful();
	}	
}