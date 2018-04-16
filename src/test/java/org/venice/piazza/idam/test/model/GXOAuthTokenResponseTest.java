package org.venice.piazza.idam.test.model;

import org.junit.Assert;
import org.junit.Test;
import org.venice.piazza.idam.model.GxOAuthTokenResponse;

public class GXOAuthTokenResponseTest {


    @Test
    public void testGXOAuthTokenResponse() {
        GxOAuthTokenResponse resp = new GxOAuthTokenResponse();

        resp.setAccessToken("access_token");
        resp.setExpiresIn(123456);
        resp.setTokenType("token_type");

        String respString = resp.toString();

        GxOAuthTokenResponse other = new GxOAuthTokenResponse();

        other.setAccessToken(resp.getAccessToken());
        other.setExpiresIn(resp.getExpiresIn());
        other.setTokenType(resp.getTokenType());

        String otherString = other.toString();

        Assert.assertEquals(respString, otherString);
    }
}
