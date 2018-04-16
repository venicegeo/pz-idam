package org.venice.piazza.idam.test.model;

import org.junit.Assert;
import org.junit.Test;
import org.venice.piazza.idam.model.GxOAuthResponse;

public class GXOAuthResponseTest {

    @Test
    public void testGxOAuthResponse() {
        GxOAuthResponse resp = new GxOAuthResponse();

        resp.setAdministrativeOrganizationCode("code");
        resp.setCommonname("common_name");
        resp.setDn("dn");
        resp.setEmail("email");
        resp.setFirstname("firstName");
        resp.setId("id");
        resp.setLastname("lastName");
        resp.setLogin("login");
        resp.setMail("mail");
        resp.setMemberof("member_of");;
        resp.setPersonatypecode("persona_type_code");
        resp.setPersonaUID("persona_uid");
        resp.setServiceOrAgency("service_or_agency");
        resp.setUid("uid");
        resp.setUri("http://my.uri");
        resp.setUsername("my_username");

        String resp1String = resp.toString();

        GxOAuthResponse other = new GxOAuthResponse();
        other.setAdministrativeOrganizationCode(resp.getAdministrativeOrganizationCode());
        other.setCommonname(resp.getCommonname());
        other.setDn(resp.getDn());
        other.setEmail(resp.getEmail());
        other.setFirstname(resp.getFirstname());
        other.setId(resp.getId());
        other.setLastname(resp.getLastname());
        other.setLogin(resp.getLogin());
        other.setMail(resp.getMail());
        other.setMemberof(resp.getMemberof());
        other.setPersonatypecode(resp.getPersonatypecode());
        other.setPersonaUID(resp.getPersonaUID());
        other.setServiceOrAgency(resp.getServiceOrAgency());
        other.setUid(resp.getUid());
        other.setUri(resp.getUri());
        other.setUsername(resp.getUsername());

        String otherString = other.toString();

        Assert.assertEquals(resp1String, otherString);
    }

}
