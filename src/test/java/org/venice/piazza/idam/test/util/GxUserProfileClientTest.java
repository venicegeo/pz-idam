package org.venice.piazza.idam.test.util;

import model.security.authz.UserProfile;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;
import org.venice.piazza.idam.model.GxAuthAResponse;
import org.venice.piazza.idam.util.GxUserProfileClient;
import util.PiazzaLogger;

import java.util.ArrayList;
import java.util.Collections;

public class GxUserProfileClientTest {

    @Mock
    PiazzaLogger logger;

    @Mock
    RestTemplate restTemplate;

    @InjectMocks
    GxUserProfileClient profileClient;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testGetUserProfileFromEx() {

        GxAuthAResponse mockResponse = new GxAuthAResponse();
        mockResponse.setNationalityextended(Collections.singletonList("nationality_1"));
        mockResponse.setServiceoragency(Collections.singletonList("my_service_or_agency"));
        mockResponse.setGxadministrativeorganizationcode(Collections.singletonList("my_admin_code"));
        mockResponse.setGxdutydodoccupationcode(Collections.singletonList("my_occupation_code"));

        Mockito.when(this.restTemplate.postForObject(
                Mockito.anyString(),
                Mockito.any(Object.class),
                Mockito.any( Class.class)))
                .thenReturn(new GxAuthAResponse[]{mockResponse});

        Assert.assertNotNull(this.profileClient.getUserProfileFromGx("my_user_name", "my_distnguished_name"));


        mockResponse.setServiceoragency(Collections.singletonList("NGA"));
        Assert.assertNotNull(this.profileClient.getUserProfileFromGx("my_user_name", "my_distnguished_name"));
        Assert.assertEquals("NGA", this.profileClient.getUserProfileFromGx("my_user_name", "my_distnguished_name").getAdminCode());
    }
}
