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
import org.springframework.context.annotation.Profile;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
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

@Component
@Profile("geoaxis")
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

	/**
	 *
	 * @param code
	 * @param redirectUri
	 * @return
	 * @throws HttpClientErrorException
	 * @throws HttpServerErrorException
	 */
	public String getAccessToken(final String code, final String redirectUri) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTHORIZATION, "Basic " + getGxBasicAuthToken());
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		MultiValueMap<String, String> map= new LinkedMultiValueMap<>();
		map.add("grant_type", "authorization_code");
		map.add("redirect_uri", redirectUri);
		map.add("code", code);
		restTemplate.getMessageConverters().add(new FormHttpMessageConverter());
		restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());
		ResponseEntity<GxOAuthTokenResponse> responseEntity = restTemplate.exchange(
				gxTokensUrl,
				HttpMethod.POST,
				new HttpEntity<>(map, headers),
				GxOAuthTokenResponse.class);
		logger.log(String.format("  Token Response Status = %s", responseEntity.getStatusCode().toString()), Severity.DEBUG);
		logger.log(String.format("  Token Response Body   = %s", responseEntity.getBody().toString()), Severity.DEBUG);
		ResponseEntity<GxOAuthTokenResponse> tokenResponse = new ResponseEntity<>(
				responseEntity.getBody(),
				responseEntity.getStatusCode());
		return tokenResponse.getBody().getAccessToken();
	}

	/**
	 *
	 * @param accessToken
	 * @return
	 * @throws HttpClientErrorException
	 * @throws HttpServerErrorException
	 */
	public ResponseEntity<GxOAuthResponse> getGxUserProfile(final String accessToken) {
		HttpHeaders headers = new HttpHeaders();
		headers.set(AUTHORIZATION, "Bearer " + accessToken);
		return new ResponseEntity<>(restTemplate.exchange(
				gxProfileUrl,
				HttpMethod.GET,
				new HttpEntity<>("parameters", headers),
				GxOAuthResponse.class).getBody(),
				HttpStatus.OK);
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
