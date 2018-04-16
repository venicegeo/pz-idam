/**
 * Copyright 2016, RadiantBlue Technologies, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/
package org.venice.piazza.idam.test.controller;

import model.response.AuthResponse;
import model.response.ErrorResponse;
import model.response.PiazzaResponse;
import model.response.UUIDResponse;
import model.security.authz.AuthorizationCheck;
import model.security.authz.Permission;
import model.security.authz.UserProfile;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;
import org.venice.piazza.idam.authn.PiazzaAuthenticator;
import org.venice.piazza.idam.authz.endpoint.EndpointAuthorizer;
import org.venice.piazza.idam.authz.throttle.ThrottleAuthorizer;
import org.venice.piazza.idam.controller.AdminController;
import org.venice.piazza.idam.controller.AuthController;
import org.venice.piazza.idam.data.DatabaseAccessor;
import org.venice.piazza.idam.model.GxOAuthResponse;
import org.venice.piazza.idam.util.GxOAuthClient;
import util.PiazzaLogger;
import util.UUIDFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

public class ControllerTests {
    @Mock
    private Environment env;
    @Mock
    private RestTemplate restTemplate;
    @Mock
    private DatabaseAccessor accessor;
    @Mock
    private UUIDFactory uuidFactory;
    @Mock
    private HttpServletRequest request;
    @Mock
    private PiazzaLogger logger;
    @Mock
    private PiazzaAuthenticator piazzaAuthenticator;
    @Mock
    private ThrottleAuthorizer throttleAuthorizer;
    @Mock
    private EndpointAuthorizer endpointAuthorizer;
    @Mock
    private GxOAuthClient oAuthClient;

    @InjectMocks
    private AdminController adminController;
    @InjectMocks
    private AuthController authenticationController;

    /**
     * Initialize mock objects.
     */
    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    /**
     * Tests root endpoint
     */
    @Test
    public void testGetHealthCheck() {
        String result = adminController.getHealthCheck();
        assertTrue(result.contains("Hello"));
    }

    @Test
    public void testGetAdminStats() {
        when(env.getActiveProfiles()).thenReturn(new String[]{"geoaxis"});
        String result = adminController.getAdminStats();
        assertTrue("{ \"profiles\":\"geoaxis\" }".equals(result));
    }

    @Test
    public void testAuthenticateUserByUUID() {
        // (1) Mock uuid is missing

        // Test
        ResponseEntity<AuthResponse> response = authenticationController.authenticateApiKey(new HashMap<String, String>());

        // Verify
        assertTrue(response.getBody() instanceof AuthResponse);
        assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue() == false);

        // (2) Mock uuid is present in request, but missing
        Map<String, String> body = new HashMap<String, String>();
        body.put("uuid", "1234");
        when(accessor.isApiKeyValid("1234")).thenReturn(false);

        UserProfile mockProfile = new UserProfile();
        mockProfile.setUsername("bsmith");
        when(accessor.getUserProfileByApiKey(Mockito.eq("1234"))).thenReturn(mockProfile);

        // Test
        response = authenticationController.authenticateApiKey(body);

        // Verify
        assertTrue(response.getBody() instanceof AuthResponse);
        assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue() == false);

        // (3) Mock uuid is present in request, and valid
        when(accessor.getUserProfileByApiKey(Mockito.eq("1234"))).thenReturn(mockProfile);
        when(accessor.isApiKeyValid("1234")).thenReturn(true);

        // Test
        response = authenticationController.authenticateApiKey(body);

        // Verify
        assertTrue(response.getBody() instanceof AuthResponse);
        assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue());
        assertTrue(((AuthResponse) response.getBody()).getUserProfile().getUsername().equals("bsmith"));

        // (4) Mock Exception thrown
        when(accessor.getUserProfileByApiKey(Mockito.eq("1234"))).thenThrow(new RuntimeException("My Bad"));
        Mockito.doNothing().when(logger).log(Mockito.anyString(), Mockito.any());

        // Test
        response = authenticationController.authenticateApiKey(body);

        // Verify
        assertTrue(response.getBody() instanceof AuthResponse);
        assertTrue(((AuthResponse) response.getBody()).getIsAuthSuccess().booleanValue() == false);
    }

    @Test
    public void testRetrieveUUID() {

        // (1) Mock - Header is missing
        when(request.getHeader("Authorization")).thenReturn(null);

        // Test
        ResponseEntity<PiazzaResponse> response = authenticationController.generateApiKey();

        // Verify
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertTrue(((ErrorResponse) (response.getBody())).message.contains("Authentication failed for user"));

        // (2) Mock - Header present, Auth fails
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        UserProfile mockProfile = new UserProfile();
        mockProfile.setUsername("testuser");
        when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass")).thenReturn(new AuthResponse(false, mockProfile));

        // Test
        response = authenticationController.generateApiKey();

        // Verify
        assertTrue(response.getBody() instanceof ErrorResponse);

        // (3) Mock - Header present, BASIC Auth succeeds, new key
        when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass")).thenReturn(new AuthResponse(true, mockProfile));
        when(uuidFactory.getUUID()).thenReturn("1234");
        when(accessor.getApiKey("testuser")).thenReturn(null);
        Mockito.doNothing().when(accessor).createApiKey("testuser", "1234");

        // Test
        response = authenticationController.generateApiKey();

        // Verify
        assertTrue(response.getBody() instanceof UUIDResponse);
        assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

        // (4) Mock - Header present, BASIC Auth succeeds, replacing key with new
        when(accessor.getApiKey("testuser")).thenReturn("4321");
        Mockito.doNothing().when(accessor).updateApiKey("testuser", "1234");

        // Test
        response = authenticationController.generateApiKey();

        // Verify
        assertTrue(response.getBody() instanceof UUIDResponse);
        assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

        // (5) Mock - Header present, PKI Auth succeeds, replacing key with new
        when(request.getHeader("Authorization"))
                .thenReturn("Basic LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tIHBlbVRlc3QgLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLTo=");
        when(piazzaAuthenticator.getAuthenticationDecision("-----BEGIN CERTIFICATE----- pemTest -----END CERTIFICATE-----"))
                .thenReturn(new AuthResponse(true, mockProfile));
        when(uuidFactory.getUUID()).thenReturn("1234");
        when(accessor.getApiKey("testuser")).thenReturn("4321");
        Mockito.doNothing().when(accessor).updateApiKey("testuser", "1234");

        // Test
        response = authenticationController.generateApiKey();

        // Verify
        assertTrue(response.getBody() instanceof UUIDResponse);
        assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

        // (6) Mock Exception
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass")).thenThrow(new RuntimeException("My Bad"));
        Mockito.doNothing().when(logger).log(Mockito.anyString(), Mockito.any());

        // Test
        response = authenticationController.generateApiKey();

        // Verify
        assertTrue(response.getBody() instanceof ErrorResponse);
        assertTrue(((ErrorResponse) (response.getBody())).message.equals("Error retrieving API Key: My Bad"));
    }

    @Test
    public void testGetExistingApiKey() {
        // (1) Mock - Header is missing
        when(request.getHeader("Authorization")).thenReturn(null);
        ResponseEntity<PiazzaResponse> response = authenticationController.getExistingApiKey();
        assertTrue(response.getBody() instanceof ErrorResponse);

        // (2) Mock - Header present, Auth fails. Username/Password
        when(request.getHeader("Authorization")).thenReturn("Basic dGVzdHVzZXI6dGVzdHBhc3M=");
        UserProfile mockProfile = new UserProfile();
        mockProfile.setUsername("testuser");
        when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass")).thenReturn(new AuthResponse(false, mockProfile));
        response = authenticationController.getExistingApiKey();
        assertTrue(response.getBody() instanceof ErrorResponse);

        // (3) Mock - Header present, Username/Password works, return Existing Key
        when(piazzaAuthenticator.getAuthenticationDecision("testuser", "testpass")).thenReturn(new AuthResponse(true, mockProfile));
        when(accessor.getApiKey("testuser")).thenReturn("1234");
        response = authenticationController.getExistingApiKey();
        assertTrue(response.getBody() instanceof UUIDResponse);
        assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

        // (4) Mock - Header present, PKI Auth succeeds, replacing key with new
        when(request.getHeader("Authorization"))
                .thenReturn("Basic LS0tLS1CRUdJTiBDRVJUSUZJQ0FURS0tLS0tIHBlbVRlc3QgLS0tLS1FTkQgQ0VSVElGSUNBVEUtLS0tLTo=");
        when(piazzaAuthenticator.getAuthenticationDecision("-----BEGIN CERTIFICATE----- pemTest -----END CERTIFICATE-----"))
                .thenReturn(new AuthResponse(true, mockProfile));
        when(accessor.getApiKey("testuser")).thenReturn("1234");
        response = authenticationController.getExistingApiKey();
        assertTrue(response.getBody() instanceof UUIDResponse);
        assertTrue(((UUIDResponse) (response.getBody())).getUuid().equals("1234"));

        // (5) Exception Handling
        when(request.getHeader("Authorization")).thenReturn("bogusheader");
        response = authenticationController.getExistingApiKey();
        assertTrue(response.getBody() instanceof ErrorResponse);

        when(request.getHeader(anyString())).thenThrow(new RuntimeException());
        this.authenticationController.getExistingApiKey();
    }

    @Test
    public void testDeleteApiKey() {
        Mockito.when(this.accessor.getUsername("my_api_key")).thenReturn("my_username");
        ResponseEntity<PiazzaResponse> goodResp = this.authenticationController.deleteApiKey("my_api_key");

        Mockito.when(this.accessor.getUsername("my_api_key")).thenThrow(new RuntimeException());
        ResponseEntity<PiazzaResponse> errorResp = this.authenticationController.deleteApiKey("my_api_key");
    }

    @Test
    public void testGenerateApiKey() {
        ResponseEntity<PiazzaResponse> resp = this.authenticationController.generateApiKeyV2();
        assertNotNull(resp);
    }

    @Test
    public void testAuthorizationEndpoint() {
        // Initialize Authorizers
        authenticationController.initializeAuthorizers();

        // 1 - Test Invalid Input: Missing username.
        AuthorizationCheck authorizationCheck = new AuthorizationCheck(null, new Permission("GET", "data"));
        ResponseEntity<AuthResponse> response = authenticationController.authenticateAndAuthorize(authorizationCheck);
        // Ensure a proper error
        assertTrue(response.getBody().getIsAuthSuccess().equals(false));

        // 2 - Test Invalid Input: API Key and username don't match.
        authorizationCheck = new AuthorizationCheck("testerA", new Permission("GET", "data"));
        authorizationCheck.setApiKey("testerBApiKey");
        when(accessor.getUsername("testerBApiKey")).thenReturn("testerB");
        response = authenticationController.authenticateAndAuthorize(authorizationCheck);
        // Ensure a proper error
        assertTrue(response.getBody().getIsAuthSuccess().equals(false));

        // 3 - Test Invalid Input: API Key is provided by invalid
        when(accessor.isApiKeyValid("testerBApiKey")).thenReturn(false);
        response = authenticationController.authenticateAndAuthorize(authorizationCheck);
        // Ensure a proper error
        assertTrue(response.getBody().getIsAuthSuccess().equals(false));

        // 4 - Test Success: Authorizers pass.
        when(throttleAuthorizer.canUserPerformAction(Mockito.any())).thenReturn(new AuthResponse(true));
        when(endpointAuthorizer.canUserPerformAction(Mockito.any())).thenReturn(new AuthResponse(true));
        authorizationCheck = new AuthorizationCheck(null, new Permission("GET", "data"));
        authorizationCheck.setApiKey("testerApiKey");
        when(accessor.isApiKeyValid("testerApiKey")).thenReturn(true);
        when(accessor.getUsername("testerApiKey")).thenReturn("testerA");
        response = authenticationController.authenticateAndAuthorize(authorizationCheck);
        // Ensure success
        assertTrue(response.getBody().getIsAuthSuccess().equals(true));

        // 5 - Test Success: Authorizers fail, and user is not authenticated
        when(throttleAuthorizer.canUserPerformAction(Mockito.any())).thenReturn(new AuthResponse(false, "Bad Stuff"));
        response = authenticationController.authenticateAndAuthorize(authorizationCheck);
        // Ensure successful denial of Authorization
        assertTrue(response.getBody().getIsAuthSuccess().equals(false));

        // 6 - Test General Exception
        response = authenticationController.authenticateAndAuthorize(null);
        // Ensure proper handling of error as a denied Authorization
        assertTrue(response.getBody().getIsAuthSuccess().equals(false));
    }

    @Test
    public void testLogin() {

        HttpSession httpSession = new MockHttpSession();
        HttpServletResponse servletResponse = new MockHttpServletResponse();

        GxOAuthResponse respBody = new GxOAuthResponse();
        respBody.setUsername("my_username");
        ResponseEntity<GxOAuthResponse> profile = new ResponseEntity<GxOAuthResponse>(respBody, HttpStatus.OK);

        String theCode = "my_code";
        Mockito.when(this.oAuthClient.getRedirectUri(this.request)).thenReturn("my_redirect_uri");
        Mockito.when(this.oAuthClient.getAccessToken(theCode, "my_redirect_uri")).thenReturn("my_access_token");
        Mockito.when(this.oAuthClient.getGxUserProfile("my_access_token")).thenReturn(profile);
        Mockito.when(this.request.getServerName()).thenReturn("my.test.domain");

        RedirectView errorView = this.authenticationController.oauthResponse(theCode, httpSession, servletResponse);

        respBody.setUid("my_uid");
        respBody.setFirstname("my_first_name");
        respBody.setLastname("my_last_name");
        RedirectView error2 = this.authenticationController.oauthResponse(theCode, httpSession, servletResponse);

        respBody.setDn("my_distinguished_name");
        RedirectView goodView = this.authenticationController.oauthResponse(theCode, httpSession, servletResponse);
    }

    @Test
    public void testLoginHttpError() {
        Mockito.when(this.oAuthClient.getRedirectUri(any(HttpServletRequest.class))).thenThrow(
                new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "status_text", "The response body".getBytes(), Charset.defaultCharset()));
        RedirectView errorView = this.authenticationController.oauthResponse("my_code", new MockHttpSession(), new MockHttpServletResponse());
    }

    @Test
    public void testLoginOtherException() {
        Mockito.when(this.oAuthClient.getRedirectUri(any(HttpServletRequest.class))).thenThrow(Exception.class);
        RedirectView errorView = this.authenticationController.oauthResponse("my_code", new MockHttpSession(), new MockHttpServletResponse());
    }

    @Test
    public void testLogout() {
        this.authenticationController.oauthLogout(new MockHttpSession());
    }
}