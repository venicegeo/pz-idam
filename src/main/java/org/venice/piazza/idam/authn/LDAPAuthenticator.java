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
import org.venice.piazza.idam.data.MongoAccessor;

import model.response.AuthenticationResponse;
import util.PiazzaLogger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;

@Component
@Profile({ "ldap" })
public class LDAPAuthenticator implements PiazzaAuthenticator {

	@Value("${SPACE}")
	private String space;

	@Value("${vcap.services.beachfront.credentials.username}")
	private String testBeachFrontUser;
	@Value("${vcap.services.beachfront.credentials.credential}")
	private String testBeachFrontCred;

	@Value("${vcap.services.pztest-integration.credentials.username}")
	private String testPzTestIntegrationUser;
	@Value("${vcap.services.pztest-integration.credentials.credential}")
	private String testPzTestIntegrationCred;

	@Value("${vcap.services.ldap.credentials.userdn}")
	private String ldapUserDN;
	@Value("${vcap.services.ldap.credentials.url}")
	private String ldapURL;
	@Value("${security.gs_ldap.ctxfactory}")
	private String ldapCtxFactory;

	@Autowired
	private MongoAccessor mongoAccessor;
	@Autowired
	private PiazzaLogger pzLogger;

	private static final Logger LOGGER = LoggerFactory.getLogger(LDAPAuthenticator.class);

	@Override
	public AuthenticationResponse getAuthenticationDecision(String username, String credential) {

		if (username == null || credential == null) {
			return new AuthenticationResponse(mongoAccessor.getUserProfileByUsername(username), false);
		} else if (isOverrideSpace() && isApprovedTestUser(username, credential)) {
			return new AuthenticationResponse(mongoAccessor.getUserProfileByUsername(username), true);
		}

		Properties env = new Properties();
		env.put(DirContext.INITIAL_CONTEXT_FACTORY, ldapCtxFactory);
		env.put(DirContext.PROVIDER_URL, ldapURL);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, "uid=" + username + "," + ldapUserDN);
		env.put(Context.SECURITY_CREDENTIALS, credential);
		try {
			DirContext dc = new InitialDirContext(env);
			dc.close();
			return new AuthenticationResponse(mongoAccessor.getUserProfileByUsername(username), true);
		} catch (NamingException ne) {
			String error = "User authentication failed for " + username;
			LOGGER.error(error, ne);
			pzLogger.log(error, PiazzaLogger.INFO);
		}

		return new AuthenticationResponse(mongoAccessor.getUserProfileByUsername(username), false);
	}

	@Override
	public AuthenticationResponse getAuthenticationDecision(String pem) {
		throw new UnsupportedOperationException("LDAP authentication does not support PKI Certificates!");
	}

	private boolean isOverrideSpace() {
		return "int".equalsIgnoreCase(space) || "stage".equalsIgnoreCase(space) || "test".equalsIgnoreCase(space)
				|| "prod".equalsIgnoreCase(space);
	}

	private boolean isApprovedTestUser(String username, String credential) {

		if (testBeachFrontUser.equals(username) && testBeachFrontCred.equals(credential)) {
			return true;
		}

		if (testPzTestIntegrationUser.equals(username) && testPzTestIntegrationCred.equals(credential)) {
			return true;
		}

		return false;
	}
}