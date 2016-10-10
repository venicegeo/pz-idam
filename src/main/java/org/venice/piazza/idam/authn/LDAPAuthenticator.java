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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import model.response.AuthenticationResponse;
import util.PiazzaLogger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;

@Component
@Profile( { "ldap" })
public class LDAPAuthenticator implements PiazzaAuthenticator {

	@Value("${SPACE}")
	private String SPACE;

	@Value("${vcap.services.beachfront.credentials.username}")
	private String TEST_BEACHFRONT_USER;
	@Value("${vcap.services.beachfront.credentials.credential}")
	private String TEST_BEACHFRONT_CRED;
	
	@Value("${vcap.services.pztest-integration.credentials.username}")
	private String TEST_PZTESTINTEGRATION_USER;
	@Value("${vcap.services.pztest-integration.credentials.credential}")
	private String TEST_PZTESTINTEGRATION_CRED;	
	
	@Value("${vcap.services.ldap.credentials.userdn}")
	private String LDAP_USER_DN;
	@Value("${vcap.services.ldap.credentials.url}")
	private String LDAP_URL;
	@Value("${security.gs_ldap.ctxfactory}")
	private String LDAP_CTX_FACTORY;
	
	@Autowired
	private PiazzaLogger logger;

	private final static Logger LOGGER = LoggerFactory.getLogger(LDAPAuthenticator.class);

	@Override
	public AuthenticationResponse getAuthenticationDecision(String username, String credential) {
		
		if (username == null || credential == null) {
			return new AuthenticationResponse(username, false);
		} else if (isOverrideSpace() && isApprovedTestUser(username, credential)) {
			return new AuthenticationResponse(username, true);
		}
		
		Properties env = new Properties();
		env.put(DirContext.INITIAL_CONTEXT_FACTORY, LDAP_CTX_FACTORY);
		env.put(DirContext.PROVIDER_URL, LDAP_URL);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "uid=" + username + "," + LDAP_USER_DN);
		env.put(Context.SECURITY_CREDENTIALS, credential);
		try {
			DirContext dc = new InitialDirContext(env);
			dc.close();
			return new AuthenticationResponse(username, true);
		} catch (NamingException ne) {
			String error = "User authentication failed for " + username;
			LOGGER.error(error, ne);
			logger.log(error, PiazzaLogger.INFO);
		}

		return new AuthenticationResponse(username, false);
	}
	
	@Override
	public AuthenticationResponse getAuthenticationDecision(String pem) {
		throw new UnsupportedOperationException("LDAP authentication does not support PKI Certificates!");
	}
	
	private boolean isOverrideSpace() {
		return SPACE.equalsIgnoreCase("int") || SPACE.equalsIgnoreCase("stage") || SPACE.equalsIgnoreCase("test") || SPACE.equalsIgnoreCase("prod");
	}
	
	private boolean isApprovedTestUser(String username, String credential) {
		
		if( TEST_BEACHFRONT_USER.equals(username) && TEST_BEACHFRONT_CRED.equals(credential)) {
			return true;
		}
		
		if( TEST_PZTESTINTEGRATION_USER.equals(username) && TEST_PZTESTINTEGRATION_CRED.equals(credential)) {
			return true;
		}
		
		return false;
	}	
}