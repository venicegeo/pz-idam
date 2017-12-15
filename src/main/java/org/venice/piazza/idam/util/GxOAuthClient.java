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
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.model.GxOAuthResponse;
import org.venice.piazza.idam.model.GxOAuthTokenResponse;

import util.PiazzaLogger;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Map;

@Component
public class GxOAuthClient {

	@Value("${GEOAXIS_AUTH}")
	private String gxAuthorizeUrl;

	@Value("${GEOAXIS_PROFILE}")
	private String gxProfileUrl;

	@Value("${GEOAXIS_LOGOUT}")
	private String gxLogoutUrl;

	@Value("${GEOAXIS_TOKENS}")
	private String gxTokensUrl;

	@Value("${GEOAXIS_CLIENT_ID}")
	private String gxClientId;

	@Value("${GEOAXIS_SECRET}")
	private String gxClientSec;

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	private PiazzaLogger logger;

	private static final String AUTHORIZATION = "Authorization";

	public String getAccessToken(final String code, final String redirectUri) throws HttpClientErrorException, HttpServerErrorException {
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTHORIZATION, "Basic " + getGxBasicAuthToken());
		ResponseEntity<GxOAuthTokenResponse> tokenResponse = new ResponseEntity<>(restTemplate.exchange(
				gxTokensUrl + "?grant_type=authorization_code&redirect_uri=" + redirectUri + "&code=" + code,
				HttpMethod.POST,
				new HttpEntity<>("parameters", headers),
				GxOAuthTokenResponse.class).getBody(),
				HttpStatus.OK);
		logger.log(tokenResponse.toString(), Severity.DEBUG);
		return tokenResponse.getBody().getAccessToken();
	}

	public ResponseEntity<GxOAuthResponse> getGxUserProfile(final String accessToken) throws HttpClientErrorException, HttpServerErrorException {
		HttpHeaders headers = new HttpHeaders();
		String encodedToken = Base64.getEncoder().encodeToString(accessToken.getBytes());
		headers.set(AUTHORIZATION, "Bearer " + encodedToken);
		ResponseEntity<GxOAuthResponse> profileResponse = new ResponseEntity<>(restTemplate.exchange(
				gxProfileUrl,
				HttpMethod.GET,
				new HttpEntity<>("parameters", headers),
				GxOAuthResponse.class).getBody(),
				HttpStatus.OK);
		return profileResponse;
	}

	public UserProfile getUserProfileFromGxProfile(GxOAuthResponse oAuthResponse) throws InvalidNameException {
		logger.log(String.format(" Profile Response = %s", oAuthResponse), Severity.DEBUG);
		final LdapName ldapName = new LdapName(oAuthResponse.getDn());
		final Rdn countryRdn = ldapName.getRdns().stream().filter(rdn ->
				rdn.getType().equalsIgnoreCase("C")
		).
				findAny().
				orElse(null);
		String country = "";
		if (countryRdn != null) {
			country = countryRdn.getValue().toString();
		}

		String dutyCode;
		String adminCode;
		if ("NGA".equalsIgnoreCase(oAuthResponse.getServiceOrAgency())) {
			dutyCode = oAuthResponse.getServiceOrAgency();
			adminCode = oAuthResponse.getServiceOrAgency();
		} else {
			dutyCode = oAuthResponse.getAdministrativeOrganizationCode();
			adminCode = oAuthResponse.getAdministrativeOrganizationCode();
		}
		final UserProfile profile = new UserProfile();
		profile.setDistinguishedName(oAuthResponse.getDn());
		profile.setUsername(oAuthResponse.getUsername());
		profile.setCountry(country);
		profile.setDutyCode(dutyCode);
		profile.setAdminCode(adminCode);
		return profile;
	}

	public String getOAuthUrlForGx(final String redirectUri)  {
		return gxAuthorizeUrl + "?scope=UserProfile.me&client_id="
				+ gxClientId + "&response_type=code&redirect_uri=" + redirectUri;
	}

	public String getLogoutUrl()  {
		return gxLogoutUrl;
	}

	public String getRedirectUri(HttpServletRequest request) {
		return getRequestBaseUrl(request) + "/login";
	}

	public String getUiUrl(HttpServletRequest request) {
		return getRequestBaseUrl(request).replace("pz-idam", "beachfront");
	}

	private String getRequestBaseUrl(HttpServletRequest request) {
		return request.getScheme() + "://" + request.getServerName();
	}

	private String getGxBasicAuthToken() {
		final String unencodedString = gxClientId + ":" + gxClientSec;
		return Base64.getEncoder().encodeToString(unencodedString.getBytes());
	}
}
