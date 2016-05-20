package org.venice.piazza.security.data;

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
public class LDAPClient {

	@Value("${vcap.services.ldap.credentials.userdn}")
	private String LDAP_USER_DN;
	@Value("${vcap.services.ldap.credentials.url}")
	private String LDAP_URL;
	@Value("${security.gs_ldap.ctxfactory}")
	private String LDAP_CTX_FACTORY;
	@Autowired
	private PiazzaLogger logger;

	public boolean getAuthenticationDecision(String username, String credential) {
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
}