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

import model.logger.Severity;
import model.security.authz.UserProfile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JacksonJsonParser;
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import util.PiazzaLogger;

import java.util.Base64;
import java.util.Map;

@Component
//@Profile({ "geoaxis" })
public class GxOAuthClient {

	//@Value("${vcap.services.geoaxis.credentials.uri}")
	private String gxBaseUrl;

	//@Value("${vcap.services.geoaxis.credentials.client_id}")
	private String gxClientId;

	//@Value("${vcap.services.geoaxis.credentials.token}")
	private String gxBasicAuthToken;


	@Autowired
	private PiazzaLogger logger;

	private static final String AUTHORIZATION = "Authorization";

	public String getAccessToken(final String code, final RestTemplate restTemplate) throws HttpClientErrorException, HttpServerErrorException {
		// String redirectUri = "bf-api.int.geointservices.io/login"
		logger.log("Test", Severity.DEBUG);
		String redirectUri = "localhost:8080/oauthResponse";
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTHORIZATION, "Basic " + getGxBasicAuthToken());
		System.out.println("restTemplate: " + restTemplate);
		ResponseEntity<String> tokenResponse = new ResponseEntity<>(restTemplate.exchange(
				getTokenRequestUrl(redirectUri, code),
				HttpMethod.POST,
				new HttpEntity<>("parameters", headers),
				String.class).getBody(),
				HttpStatus.OK);
		String accessToken = tokenResponse.getBody();
		JacksonJsonParser parser = new JacksonJsonParser();
		Map<String, Object> accessTokenMap = parser.parseMap(accessToken);
		return accessTokenMap.get("access_token").toString();
	}

	public ResponseEntity<String> getUserProfile(final String accessToken, final RestTemplate restTemplate) throws HttpClientErrorException, HttpServerErrorException {
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTHORIZATION, "Bearer " + accessToken);
		System.out.println("restTemplate: " + restTemplate);
		ResponseEntity<String> profileResponse = new ResponseEntity<>(restTemplate.exchange(
				getProfileRequestUrl(),
				HttpMethod.GET,
				new HttpEntity<>("parameters", headers),
				String.class).getBody(),
				HttpStatus.OK);
		return profileResponse;
	}

	public String getOAuthUrlForGx()  {
		final String redirectUrl = "localhost:8080/oauthResponse";
		gxBaseUrl = "localhost:5001";
		gxClientId = "XXX";
		final String url = "http://" + gxBaseUrl + "/ms_oauth/oauth2/endpoints/oauthservice/authorize?scope=UserProfile.me&client_id="
				+ gxClientId + "&response_type=code&redirect_uri=" + redirectUrl;
		return url;
	}

	private String getTokenRequestUrl(
			final String redirectUri,
			final String code) {
		gxBaseUrl = "localhost:5001";
		final String url = "http://" + gxBaseUrl + "/ms_oauth/oauth2/endpoints/oauthservice/tokens?grant_type=authorization_code&redirect_uri="
				+ redirectUri + "&code=" + code;
		return url;
	}

	private String getProfileRequestUrl() {
		gxBaseUrl = "localhost:5001";
		final String url = "http://" + gxBaseUrl + "/ms_oauth/resources/userprofile/me";
		return url;
	}

	private String getGxBasicAuthToken() {
		gxClientId = "XXX";
		final String gxClientSec = "YYY";
		final String unencodedString = gxClientId + ":" + gxClientSec;
		return Base64.getEncoder().encodeToString(unencodedString.getBytes());
	}
}
