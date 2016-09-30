package org.venice.piazza.idam.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import util.PiazzaLogger;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Properties;

@Component
public class LDAPAccessor {

	@Value("${SPACE}")
	private String SPACE;
	@Value("${vcap.services.ldap.credentials.userdn}")
	private String LDAP_USER_DN;
	@Value("${vcap.services.ldap.credentials.url}")
	private String LDAP_URL;
	@Value("${security.gs_ldap.ctxfactory}")
	private String LDAP_CTX_FACTORY;
	
	@Value("${vcap.services.pz-servicecontroller.credentials.username}")
	private String SYSTEM_PZSERVICECONTROLLER_USER;
	@Value("${vcap.services.pz-servicecontroller.credentials.credential}")
	private String SYSTEM_PZSERVICECONTROLLER_CRED;
	
	@Value("${vcap.services.beachfront.credentials.username}")
	private String TEST_BEACHFRONT_USER;
	@Value("${vcap.services.beachfront.credentials.credential}")
	private String TEST_BEACHFRONT_CRED;
	
	@Value("${vcap.services.pztest-integration.credentials.username}")
	private String TEST_PZTESTINTEGRATION_USER;
	@Value("${vcap.services.pztest-integration.credentials.credential}")
	private String TEST_PZTESTINTEGRATION_CRED;
	
	@Autowired
	private PiazzaLogger logger;

	public boolean getAuthenticationDecision(String username, String credential) {
		if (username == null || credential == null) {
			return false;
		} else if ((isOverrideSpace() && isApprovedTestUser(username, credential)) || isApprovedSystemUser(username, credential)) {
			return true;
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
			return true;
		} catch (NamingException ne) {
			logger.log("User authentication failed for " + username, PiazzaLogger.INFO);
		}
		return false;
	}

	private boolean isOverrideSpace() {
		return (SPACE.equalsIgnoreCase("int") || SPACE.equalsIgnoreCase("stage") || SPACE.equalsIgnoreCase("test") || SPACE.equalsIgnoreCase("prod"));
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
	
	private boolean isApprovedSystemUser(String username, String credential) {
		
		if( SYSTEM_PZSERVICECONTROLLER_USER.equals(username) && SYSTEM_PZSERVICECONTROLLER_CRED.equals(credential)) {
			return true;
		}
		
		return false;
	}
	
	public boolean isSystemUser(String username) {
		
		if( SYSTEM_PZSERVICECONTROLLER_USER.equals(username) ) {
			return true;
		}
		
		return false;
	}	
}
