package org.venice.piazza.idam.test.model;

import org.junit.Assert;
import org.junit.Test;
import org.venice.piazza.idam.model.GxAuthAResponse;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

public class GXAuthAResponseTest {

    @Test
    public void testGXAuthoAResponse() {
        GxAuthAResponse resp = new GxAuthAResponse();

        resp.setServiceoragency(Collections.singletonList("service"));
        resp.setGxadministrativeorganizationcode(Collections.singletonList("organization"));
        resp.setGxdutydodoccupationcode(Collections.singletonList("occupation"));
        resp.setNationalityextended(Collections.singletonList("nationality"));

        String respString = resp.toString();

        GxAuthAResponse other = new GxAuthAResponse();
        other.setServiceoragency(resp.getServiceoragency());
        other.setGxadministrativeorganizationcode(resp.getGxadministrativeorganizationcode());
        other.setGxdutydodoccupationcode(resp.getGxdutydodoccupationcode());
        other.setNationalityextended(resp.getNationalityextended());

        String otherString = other.toString();

        Assert.assertEquals(respString, otherString);
    }
}
