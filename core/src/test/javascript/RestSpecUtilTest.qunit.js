/*jslint newcap: false*/
/*global $, module, equal, raises, test, RuntimeException, RestSpecUtil, MockHttpClient, XMLSerializer */

define(["RestSpecUtil"], function(){
    module("RestSpecUtilTest");

    test("With Ampersand getSpec not wrapped throws exception", function () {
        // Given
        var spec, MockRestSpec = [], error;

        MockRestSpec.push({
            "name": "find it",
            "url": "/wacky url time with spaces",
            "response": {
                "header":{"Content-Type":"application/xml"},
                "representation": "<results><ad><name>Hello & Fozzy></name></ad></results>"
            }
        });
        try {
            RestSpecUtil(MockRestSpec).getSpec("find it");
        } catch (except) {
            error = except;
        }
        
        equal(error, "Your response representation may not be well-formed.  Possible cause: an XML marker tag like < or & not wrapped in a CDATA");
    });

    test("json response is okay", function () {
        // Given
        var spec, MockRestSpec = [], restSpec, json;

        MockRestSpec.push({
            "name": "super duper",
            "url": "/superduper.json",
            "response": {
                "header":{"Content-Type":"application/json"},
                "representation": "{\"super\":\"duper\",\"supper\":1}"
            }
        });
        restSpec = RestSpecUtil(MockRestSpec).getSpec("super duper");

        equal(restSpec.response.representation.asXml, "{\"super\":\"duper\",\"supper\":1}");
        equal(restSpec.response.representation.asText, "{\"super\":\"duper\",\"supper\":1}");
        json = restSpec.response.representation.asJson
        equal(json.super, "duper");
        equal(json.supper, 1);

    });

    test("text response is okay", function () {
        // Given
        var spec, MockRestSpec = [], restSpec, json;

        MockRestSpec.push({
            "name": "super duper",
            "url": "/superduper.json",
            "response": {
                "header":{"Content-Type":"text/plain"},
                "representation": "whatever, it doesn't matter"
            }
        });

        // when
        restSpec = RestSpecUtil(MockRestSpec).getSpec("super duper");

        // then
        equal(restSpec.response.representation.asText, "whatever, it doesn't matter");
    });

    test("Without Ampersand getSpec with different representations", function () {
        // Given
        var spec, MockRestSpec = [];

        MockRestSpec.push({
            "name": "find it",
            "url": "/wacky url time with spaces",
            "response": {
                "header":{"Content-Type":"application/xml"},
                "representation": "<results><ad><name>Hello &amp; Fozzy <![CDATA[& Kermit]]></name><id>1</id><description><![CDATA[http://fun.html]]></description></ad></results>"
            }
        });
        spec = RestSpecUtil(MockRestSpec).getSpec("find it");

        equal(typeof spec.response.representation.asXml, "object");
        equal($(spec.response.representation.asXml).text(),  "Hello & Fozzy & Kermit1http://fun.html");
        equal(new XMLSerializer().serializeToString(spec.response.representation.asXml), "<results><ad><name>Hello &amp; Fozzy <![CDATA[& Kermit]]></name><id>1</id><description><![CDATA[http://fun.html]]></description></ad></results>");
    });

    test("RestSpecUtil gets a spec", function () {
        var MockRestSpec = [], spec;

        MockRestSpec.push({
            "name": "don't respond to this",
            "response": {
                "outcome": "you lose!"
            }
        });

        MockRestSpec.push({
            "what is this?": "an almost empty object"
        });

        MockRestSpec.push({
            "name": "respond to this",
            "response": {
                "outcome": "option A"
            }
        });

        MockRestSpec.push({
            "name": "respond to this",
            "response": {
                "outcome": "option B"
            }
        });

        spec = RestSpecUtil(MockRestSpec).getSpec("respond to this");
        equal(spec.response.outcome, "option A");
    });

    test("RestSpecUtil replaces text in the url", function () {
        var MockRestSpec = [], spec;

        MockRestSpec.push({
            "name": "respond to this",
            "url": "/map/url/{replaceMe}/okay"
        });

        spec = RestSpecUtil(MockRestSpec).getSpec("respond to this", {"{replaceMe}": "newText"});
        equal(spec.url, "/map/url/newText/okay");

        spec = RestSpecUtil(MockRestSpec).getSpec("respond to this", {"{replaceMe}": "secondNewText"});
        equal(spec.url, "/map/url/secondNewText/okay");
    });

    test("RestSpecUtil can replace multiple placeholders in the url", function () {
        var MockRestSpec = [], spec;

        MockRestSpec.push({
            "name": "respond to this",
            "url": "/map/{replaceMe}/url/{replaceMe}/okay/{meToo}/done"
        });

        spec = RestSpecUtil(MockRestSpec).getSpec("respond to this", {"{replaceMe}": "newText", "{meToo}": "replacementX"});
        equal(spec.url, "/map/newText/url/newText/okay/replacementX/done");
    });

    test("RestSpecUtil returns nothing when looking for a name that is not present", function () {
        var MockRestSpec = [], spec;
        MockRestSpec.push({"name": "not gonna look for this"});
        spec = RestSpecUtil(MockRestSpec).getSpec("look for something else");
        equal(typeof spec, "undefined", "The type of the returned spec should be undefined (the spec wasn't found)");

        spec = RestSpecUtil(MockRestSpec).getSpec("look for something else", {"gotta replace": "this for that"});
        equal(typeof spec, "undefined", "The type of the returned spec should be undefined (the spec wasn't found)");
    });

    test("RestSpecUtil returns the spec even when url is not present", function () {
        var MockRestSpec = [], spec;
        MockRestSpec.push({"name": "bad spec"});
        spec = RestSpecUtil(MockRestSpec).getSpec("bad spec");
        equal(spec.name, "bad spec");
        spec = RestSpecUtil(MockRestSpec).getSpec("bad spec", {"replace": "with"});
        equal(spec.name, "bad spec");
    });

    test("RestSpecUtil replaces response.representation with the asXXX nodes MockHttpClient expects", function () {
        var MockRestSpec = [], spec;
        MockRestSpec.push({
            "name": "find it",
            "url": "/wacky url time with spaces",
            "response": {
                "header":{"Content-Type":"application/xml"},
                "representation": "<text>where am I?</text>"
            }
        });

        spec = RestSpecUtil(MockRestSpec).getSpec("find it");

        equal(typeof spec.response.representation, "object");
        equal(typeof spec.response.representation.asXml, "object");
        equal(typeof spec.response.representation.asJson, "string");
        equal(typeof spec.response.representation.asText, "string");

        equal(new XMLSerializer().serializeToString(spec.response.representation.asXml), "<text>where am I?</text>");
        equal(spec.response.representation.asJson, "<text>where am I?</text>");
        equal(spec.response.representation.asText, "<text>where am I?</text>");
        
        spec = RestSpecUtil(MockRestSpec).getSpec("find it");
        equal(new XMLSerializer().serializeToString(spec.response.representation.asXml), "<text>where am I?</text>");
    });

    test("RestSpec retrieves a file when asked nicely", function () {
        var mockRestSpecs = [],
            mockClient,
            expectedRequestRepresentation = "{'columns':[]}",
            expectedResponseRepresentation = "<text>pizza pie</text>",
            spec,
            util;

        mockRestSpecs.push({
            "name": "find it",
            "url": "/wacky url time with spaces",
            "request": {
                "header":{"Content-Type":"application/json"},
                "representation-ref": "/line/package/stuff/requestContent.json"
            },
            "response": {
                "header":{"Content-Type":"application/xml"},
                "representation-ref": "/line/package/stuff/responseContent.xml"
            }
        });

        mockClient = {
            ajax: function(opts) {
                if(opts.url === "/line/package/stuff/requestContent.json") {
                    opts.success("whatever", "text status", {responseText: expectedRequestRepresentation});
                } else if (opts.url === "/line/package/stuff/responseContent.xml"){
                    opts.success("whatever", "text status", {responseText: expectedResponseRepresentation});
                }
            }
        };

        util = RestSpecUtil(mockRestSpecs, {client:mockClient});
        spec = util.getSpec("find it");

        ok(spec.request.representation, 'request.representation exists');
        ok(spec.response.representation, 'response.representation exists');

        equal(spec.request.representation.asText, expectedRequestRepresentation, 'request representation gives ref content');
        equal(spec.response.representation.asText, expectedResponseRepresentation, 'response representation gives ref content');
    });

    test("RestSpecUtil resolves file-refs relative to the pathPrefix option if present", function(){
        // GIVEN: a spec with a file ref
        var MockRestSpec = [], mockClient, retrieved, spec, util;
        MockRestSpec.push({
            "name": "find it",
            "url": "/wacky url time with spaces",
            "response": {
                "header":{"Content-Type":"application/xml"},
                "representation-ref": "/line/package/stuff/somefile.xml"
            }
        });
        
        mockClient = {
                ajax: function(opts) {
                    if(opts.url === "/line/package/stuff/somefile.xml") {
                        opts.success("whatever", "text status", {responseText: "<message>want some frogs instead?</message>"});
                    } else if(opts.url === "/fuzzybunnies/line/package/stuff/somefile.xml"){
                        opts.success("whatever", "text status", {responseText: "<message>these are the bunnies you were looking for</message>"});
                    }
                }
            };

        util = RestSpecUtil(MockRestSpec, {client:mockClient, pathPrefix:"/fuzzybunnies"});
        // WHEN: RestSpecUtil is asked to resolve that file ref relative to a pathPrefix
        spec = util.getSpec("find it");

        
        // THEN: it should find the file relative to the prefix
        retrieved = util.getRepresentationRefText(spec.response["representation-ref"]);
        equal(retrieved, "<message>these are the bunnies you were looking for</message>");
        
    });
});
