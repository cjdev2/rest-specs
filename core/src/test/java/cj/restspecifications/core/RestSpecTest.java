/**
 * Copyright (C) 2011, 2012, 2013 Commission Junction Inc.
 *
 * This file is part of rest-specs.
 *
 * rest-specs is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * rest-specs is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with rest-specs; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library.  Thus, the terms and
 * conditions of the GNU General Public License cover the whole
 * combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules to produce an
 * executable, regardless of the license terms of these independent
 * modules, and to copy and distribute the resulting executable under
 * terms of your choice, provided that you also meet, for each linked
 * independent module, the terms and conditions of the license of that
 * module.  An independent module is a module which is not derived from
 * or based on this library.  If you modify this library, you may extend
 * this exception to your version of the library, but you are not
 * obligated to do so.  If you do not wish to do so, delete this
 * exception statement from your version.
 */
package cj.restspecifications.core;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import cj.restspecs.core.io.StringLoader;
import org.junit.Test;

import cj.restspecs.core.RestSpec;
import cj.restspecs.core.io.Loader;
import cj.restspecs.core.model.Header;

public class RestSpecTest {

    @Test
    public void contentTypeHeaderIsAccesibleViaAConvenienceMethondOnTheResponse() {
        // given
        final String specJson =
                "{\n" +
                        "    \"name\":\"some spec\",\n" +
                        "    \"url\":\"/some/path\",\n" +
                        "    \"request\": {\n" +
                        "        \"method\": \"GET\"\n" +
                        "    },\n" +
                        "    \"response\":{\n" +
                        "        \"statusCode\": 200,\n" +
                        "        \"header\":{\"Content-Type\":\"fuzzy/bannanas\"},\n" +
                        "        \"representation\":\"wahoo!\"\n" +
                        "    }\n" +
                        "}\n";

        Loader dummyLoader = new Loader() {
            public InputStream load(String name) {
                return new ByteArrayInputStream(specJson.getBytes());
            }
        };
        RestSpec spec = new RestSpec("a.spec.json", dummyLoader);

        // when
        String contentType = spec.response().representation().contentType();

        // then
        assertEquals("fuzzy/bannanas", contentType);
    }

    @Test
    public void queryStringsDefaultToAnEmptyStringIfNotPresent() {
        // given
        final String specJson =
                "{\n" +
                        "    \"name\":\"some spec\",\n" +
                        "    \"url\":\"/some/path\",\n" +
                        "    \"request\": {\n" +
                        "        \"method\": \"GET\"\n" +
                        "    },\n" +
                        "    \"response\":{\n" +
                        "        \"statusCode\": 404\n" +
                        "    }\n" +
                        "}";

        Loader dummyLoader = new Loader() {
            public InputStream load(String name) {
                return new ByteArrayInputStream(specJson.getBytes());
            }
        };

        RestSpec spec = new RestSpec("a.spec.json", dummyLoader);

        // when
        String query = spec.queryString();

        // then
        assertEquals("", query);
    }

    @SuppressWarnings("serial")
    @Test
    public void queryStringsAreParsedIntoKeyValuePairs() {
        // given
        final String specJson =
                "{\n" +
                        "    \"name\":\"some spec\",\n" +
                        "    \"url\":\"/some/path?someVar=someValue&someOtherVar=someOtherValue&theLastVar=yetAnotherValue\",\n" +
                        "    \"request\": {\n" +
                        "        \"method\": \"GET\"\n" +
                        "    },\n" +
                        "    \"response\":{\n" +
                        "        \"statusCode\": 404\n" +
                        "    }\n" +
                        "}\n";
        Loader dummyLoader = new Loader() {
            public InputStream load(String name) {
                return new ByteArrayInputStream(specJson.getBytes());
            }
        };
        RestSpec spec = new RestSpec("a.spec.json", dummyLoader);

        // when
        Map<String, String> params = spec.queryParams();

        // then
        Map<String, String> expected = new HashMap<String, String>() {{
            put("someVar", "someValue");
            put("someOtherVar", "someOtherValue");
            put("theLastVar", "yetAnotherValue");
        }};
        assertEquals(expected, params);
    }

    @Test
    public void queryStringsAreParsedOutOfTheUrl() {
        // given
        final String specJson =
                "{\n" +
                        "    \"name\":\"some spec\",\n" +
                        "    \"url\":\"/some/path?someVar=someValue\",\n" +
                        "    \"request\": {\n" +
                        "        \"method\": \"GET\"\n" +
                        "    },\n" +
                        "    \"response\":{\n" +
                        "        \"statusCode\": 404\n" +
                        "    }\n" +
                        "}";
        Loader dummyLoader = new Loader() {
            public InputStream load(String name) {
                return new ByteArrayInputStream(specJson.getBytes());
            }
        };
        RestSpec spec = new RestSpec("a.spec.json", dummyLoader);

        // when
        String query = spec.queryString();

        // then
        assertEquals("?someVar=someValue", query);
    }


    @Test
    public void removesQueryStringsFromThePathWhenAsked() {
        // given
        final String specJson =
                "{\n" +
                        "    \"name\":\"some spec\",\n" +
                        "    \"url\":\"/some/path?someVar=someValue\",\n" +
                        "    \"request\": {\n" +
                        "        \"method\": \"GET\"\n" +
                        "    },\n" +
                        "    \"response\":{\n" +
                        "        \"statusCode\": 404\n" +
                        "    }\n" +
                        "}";

        Loader dummyLoader = new Loader() {
            public InputStream load(String name) {
                return new ByteArrayInputStream(specJson.getBytes());
            }
        };
        RestSpec spec = new RestSpec("a.spec.json", dummyLoader);

        // when
        String query = spec.pathMinusQueryStringAndFragment();

        // then
        assertEquals("/some/path", query);
    }

    @Test
    public void testRestSpecJsonUtilLoadFile() throws Exception {

        RestSpec spec = new RestSpec("/cj/restspecifications/test/FunSpec.spec.json");
        assertEquals("/path", spec.path());
        String specResponse = spec.response().representation().asText().trim(); // trim the new line.
        assertEquals("contents", specResponse);
        assertEquals(300, spec.response().statusCode());

        String specRequest = spec.request().representation().asText();
        assertEquals("hippo", specRequest);

        assertEquals("PUT", spec.request().method());
        assertEquals("the resource for fun", spec.name());
    }

    @Test
    public void givesUsersAWayToListTheHeaders() throws Exception {
        // GIVEN: a spec for a get request with no representation
        final String specJson = (
                "{\n" +
                        "    \"name\":\"some spec\",\n" +
                        "    \"url\":\"/some/path\",\n" +
                        "    \"request\": {\n" +
                        "        \"method\": \"GET\",\n" +
                        "        \"header\": {\n" +
                        "            \"Green Eggs\": \"I'm sam\",\n" +
                        "            \"And Ham\": \"sam I am\"\n" +
                        "        }\n" +
                        "    },\n" +
                        "    \"response\":{\n" +
                        "        \"statusCode\": 201,\n" +
                        "        \"header\": {\n" +
                        "            \"FooName\": \"foo value\",\n" +
                        "            \"BarName\": \"bar value\"\n" +
                        "        }\n" +
                        "    }\n" +
                        "}\n");
        System.out.println(specJson);
        Loader dummyLoader = new Loader() {
            public InputStream load(String name) {
                return new ByteArrayInputStream(specJson.getBytes());
            }
        };

        // WHEN:
        RestSpec spec = new RestSpec("dummyName", dummyLoader);

        // THEN:

        String[][] expectedRequestHeaders = {
                {"Green Eggs", "I'm sam"},
                {"And Ham", "sam I am"}};

        assertHeaders(expectedRequestHeaders, spec.request().header());

        String[][] expectedResponseHeaders = {
                {"FooName", "foo value"},
                {"BarName", "bar value"}};

        assertHeaders(expectedResponseHeaders, spec.response().header());
    }

    private void assertHeaders(String[][] expected, Header actual) {
        List<String> names = actual.fieldNames();
        assertEquals(expected.length, names.size());
        for (int x = 0; x < expected.length; x++) {
            String expectedName = expected[x][0];
            String actualName = names.get(x);
            assertEquals(expectedName, actualName);

            String expectedValue = expected[x][1];
            List<String> actualValues = actual.fieldsNamed(expectedName);
            assertEquals(1, actualValues.size());
            assertEquals(expectedValue, actualValues.get(0));
        }
    }

    @Test
    public void givesUsersAMeansOfDetectingWhetherTheSpecHasARequestRepresentation() throws Exception {
        String[] waysToExpressRepresentation = {
                "\"representation\":\"sometext\"",
                "\"representation-ref\":\"somefile\""
        };
        for (String representationLine : waysToExpressRepresentation) {
            // GIVEN: a spec for a get request with no representation
            final String specJson = (
                    "{\n" +
                            "    \"name\":\"some spec\",\n" +
                            "    \"url\":\"/some/path\",\n" +
                            "    \"request\": {\n" +
                            "        \"method\": \"GET\"\n" +
                            "    },\n" +
                            "    \"response\":{\n" +
                            "        representationLine, \n" +
                            "        \"statusCode\": 200\n" +
                            "    }\n" +
                            "}\n").replaceAll("representationLine", Matcher.quoteReplacement(representationLine));

            Loader dummyLoader = new StringLoader(specJson);

            // WHEN: you read it with a 'RestSpec' instance
            RestSpec spec = new RestSpec("dummyName", dummyLoader);

            // THEN: you should be able to figure out whether it has a representation on the request
            assertTrue("There should be no representation", spec.request().representation() == null);
        }
    }

    @Test
    public void giveUsersACleanCodeWayOfBuildingPathReplacements() {
        String specJson = "{ \"url\": \"/path/{replacement1}\" }";

        RestSpec restSpecWithoutReplacements = new RestSpec("coolHandSpec", new StringLoader(specJson));
        RestSpec restSpecWithPathReplacement = restSpecWithoutReplacements.withParameter("{replacement1}", "luke");

        assertThat("RestSpec should be immutable", restSpecWithoutReplacements, not(sameInstance(restSpecWithPathReplacement)));
        assertThat(restSpecWithPathReplacement.replacedPath(), equalTo("/path/luke"));
    }

    @Test
    public void pathReplacementWorksWithQueryParametersAsWell() {
        String specJson = "{ \"url\": \"/path?leftParameter={left}&rightParameter={right}\" }";

        RestSpec restSpecWithoutReplacements = new RestSpec("testSpec", new StringLoader(specJson));
        RestSpec restSpecWithLeftReplacement = restSpecWithoutReplacements.withParameter("{left}", "lefty");
        RestSpec restSpecWithRightReplacement = restSpecWithLeftReplacement.withParameter("{right}", "righty");

        assertThat("RestSpec should be immutable", restSpecWithoutReplacements, not(sameInstance(restSpecWithLeftReplacement)));
        assertThat("RestSpec should be immutable", restSpecWithoutReplacements, not(sameInstance(restSpecWithRightReplacement)));
        assertThat("RestSpec should be immutable", restSpecWithLeftReplacement, not(sameInstance(restSpecWithRightReplacement)));

        assertThat(restSpecWithLeftReplacement.replacedPath(), equalTo("/path?leftParameter=lefty&rightParameter={right}"));
        assertThat(restSpecWithRightReplacement.replacedPath(), equalTo("/path?leftParameter=lefty&rightParameter=righty"));
    }

    @Test
    public void cannotCreateARestSpecWithoutAURL() throws Exception {
        String lonelySpecJson = "{}";

        try {
            new RestSpec("lonelySpecJson", new StringLoader(lonelySpecJson));
            fail("Did not validate url presence in spec");
        } catch (RuntimeException cause) {
            assertThat(cause.getMessage(), equalTo("Spec is missing a 'url'"));
        }
    }

    @Test
    public void getAllQueryParameterNamesFromUrl() {
        String lotsaNamesSpecJson = "{ \"url\": \"/?a=b&b=c&c=d\" }";
        RestSpec spec = new RestSpec("lotsaNamesSpecJson", new StringLoader(lotsaNamesSpecJson));

        List<String> names = spec.queryParameters().names();
        assertThat(names.size(), equalTo(3));
        assertThat(names.get(0), equalTo("a"));
        assertThat(names.get(1), equalTo("b"));
        assertThat(names.get(2), equalTo("c"));
    }

    @Test
    public void urlContainsMultipleQueryParameters() {
        String montyHallSpecJson = "{ \"url\": \"/door?name=a&other=b\" }";
        RestSpec spec = new RestSpec("montyHallSpecJson", new StringLoader(montyHallSpecJson));

        assertThat(spec.queryParameters().names(), equalTo(asList("name", "other")));
        assertThat(spec.queryParameterValue("other"), equalTo("b"));
        assertThat(spec.queryParameterValue("name"), equalTo("a"));
    }

    @Test
    public void whenUrlContainsMultipleQueryParametersOfTheSameName() {
        String dittoSpecJson = "{ \"url\": \"/ditto?answer=yes&answer=no&answer=maybe\" }";
        RestSpec spec = new RestSpec("dittoSpecJson", new StringLoader(dittoSpecJson));

        assertThat(spec.queryParameters().names(), equalTo(asList("answer")));
        assertThat(spec.queryParameters().values("answer"), equalTo(asList("yes", "no", "maybe")));
    }

    @Test
    public void whenAskingForAQueryParameterNameThatDoesNotExistThrowAnExceptionBecauseYouProbablyHaveATestLogicError() {
        String badSpecJson = "{ \"url\": \"/spelling?mistake=not-me\" }";
        RestSpec spec = new RestSpec("badSpecJson", new StringLoader(badSpecJson));

        try {
            spec.queryParameterValue("misteak");
            fail("Should have thrown an exception invoking queryParameterValue because the parameter 'misteak' does not exist");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), equalTo("Parameter name 'misteak' not found in specification."));
        }

        try {
            spec.queryParameters().values("misstake");
            fail("Should have thrown an exception invoking queryParameterValues because the parameter 'misstake' does not exist");
        } catch (RuntimeException expected) {
            assertThat(expected.getMessage(), equalTo("Parameter name 'misstake' not found in specification."));
        }
    }
}
