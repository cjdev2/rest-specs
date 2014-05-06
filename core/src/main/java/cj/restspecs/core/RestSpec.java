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
package cj.restspecs.core;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import cj.restspecs.core.io.ClasspathLoader;
import cj.restspecs.core.io.Loader;
import cj.restspecs.core.model.Header;
import cj.restspecs.core.model.Representation;
import cj.restspecs.core.model.Request;
import cj.restspecs.core.model.Response;

public class RestSpec {
    private JsonNode root;
    private final String name, url;
    private final Loader loader;
    private final Map<String, Object> replacements;
    private final QueryParameters queryParameters;

    private RestSpec(RestSpec originalSpec, Map<String, Object> replacements) {
        this.root = originalSpec.root;
        this.name = originalSpec.name;
        this.url = originalSpec.url;
        this.loader = originalSpec.loader;

        this.replacements = new HashMap<String, Object>();
        this.replacements.putAll(originalSpec.replacements);
        this.replacements.putAll(replacements);
        this.queryParameters = new QueryParameters(queryString());
    }

    public RestSpec(String specName) {
        this(specName, new ClasspathLoader());
    }

    public RestSpec(String specName, Loader loader) {
        this.loader = loader;
        this.replacements = new HashMap<String, Object>();

        InputStream is = loader.load(specName);
        if (is == null) {
            throw new RuntimeException("Could not find file named " + specName);
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            root = mapper.readValue(is, JsonNode.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        name = root.path("name").getValueAsText();
        url = root.path("url").getValueAsText();

        if (url == null) {
            throw new RuntimeException("Spec is missing a 'url'");
        }

        blowUpIfThereAreFieldsBesidesThese(root, Arrays.asList(
                "name",
                "url",
                "request",
                "response"
        ));
        blowUpIfThereAreFieldsBesidesThese(root.path("response"), Arrays.asList(
                "statusCode",
                "header",
                "representation",
                "representation-ref"
        ));
        queryParameters = new QueryParameters(queryString());
    }

    private void blowUpIfThereAreFieldsBesidesThese(JsonNode root, List<String> allowedNodes) {
        Iterator<String> fields = root.getFieldNames();
        while (fields.hasNext()) {
            String field = fields.next();
            if (!allowedNodes.contains(field)) {
                throw new RuntimeException("Field '" + field + "' is not allowed");
            }
        }
    }

    public String name() {
        return name;
    }

    public String pathMinusQueryStringAndFragment() {
        return parseUrl()[0];
    }

    private String[] parseUrl() {
        String replacedUrl = replacedPath();
        int delimiterPos = replacedUrl.indexOf('?');

        String path = replacedUrl;
        String queryString = "";

        if (delimiterPos > -1) {
            path = replacedUrl.substring(0, delimiterPos);
            queryString = replacedUrl.substring(delimiterPos);
        }

        return new String[] {
            path,
            queryString
        };
    }

    public class QueryParameters {
        private final List<String> namesInOrder;
        private final Map<String, List<String>> memoizedQueryParameters;

        private QueryParameters(String queryString) {
            namesInOrder = new ArrayList<String>();
            memoizedQueryParameters = new HashMap<String, List<String>>();

            if (queryString != "") {
                String queryWithoutInitialDelimiter = queryString.substring(1);
                String[] parameters = queryWithoutInitialDelimiter.split("&");
                for (String parameter : parameters) {
                    String[] pair = parameter.split("=");
                    String key = decodeUrlString(pair[0]);
                    String value = "";
                    if (pair.length > 1) {
                        value = decodeUrlString(pair[1]);
                    }

                    if (!memoizedQueryParameters.containsKey(key)) {
                        memoizedQueryParameters.put(key, new ArrayList<String>());
                        namesInOrder.add(key);
                    }

                    List<String> values;
                    values = memoizedQueryParameters.get(key);
                    values.add(value);
                }
            }
        }

        public List<String> names() {
            return namesInOrder;
        }

        public String value(String name) {
            return values(name).get(0);
        }

        public List<String> values(String name) {
            if (memoizedQueryParameters.containsKey(name)) {
                return memoizedQueryParameters.get(name);
            } else {
                String parameterNotFoundMessage = String.format("Parameter name '%s' not found in specification.", name);
                throw new RuntimeException(parameterNotFoundMessage);
            }
        }
    }

    private String decodeUrlString(String input) {
        try {
            String result;
            result = URLDecoder.decode(input, "UTF-8");
            return result;
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not decode: " + input, e);
        }
    }

    public String queryParameterValue(String parameterName) {
        return queryParameters.value(parameterName);
    }

    public QueryParameters queryParameters() {
        return queryParameters;
    }

    /**
     * @deprecated This design does not take into account multi-valued parameters.
     */
    @Deprecated
    public Map<String, String> queryParams() {
        Map<String, String> queryParams = new HashMap<String, String>();
        String query = queryString();
        if (query != "") {
            String queryWithoutInitialDelimiter = query.substring(1);
            String[] parameters = queryWithoutInitialDelimiter.split("&");
            for (String parameter : parameters) {
                String[] pair = parameter.split("=");

                String key = decodeUrlString(pair[0]);
                String value = null;
                if (pair.length > 1) {
                    value = decodeUrlString(pair[1]);
                }

                queryParams.put(key, value);
            }
        }

        return queryParams;
    }

    public String queryString() {
        return parseUrl()[1];
    }

    public String path() {
        return url;
    }

    public String getPathReplacedWith(Map<String, ?> replacements) {
        String urlWithReplacedValues = url;
        for (Map.Entry<String, ?> entry : replacements.entrySet()) {
            urlWithReplacedValues = urlWithReplacedValues.replace(entry.getKey().toString(), entry.getValue().toString());
        }
        return urlWithReplacedValues;
    }

    public Request request() {
        return new RequestFromRestSpec(root, loader);
    }

    public Response response() {
        final JsonNode responseNode = root.path("response");
        if (responseNode.isMissingNode()) {
            throw new RuntimeException("Spec is missing a 'response'");
        }

        return new ResponseFromRestSpec(responseNode, loader);
    }

    public RestSpec withParameter(String parameterName, Object parameterValue) {
        HashMap<String, Object> replacements;
        replacements = new HashMap<String, Object>();
        replacements.put(parameterName, parameterValue);

        return new RestSpec(this, replacements);
    }

    public String replacedPath() {
        return getPathReplacedWith(replacements);
    }
}

class HeaderImpl implements Header {
    JsonNode headerNode;

    HeaderImpl(JsonNode headerNode) {
        this.headerNode = headerNode;
    }

    public List<String> fieldNames() {

        List<String> results = new ArrayList<String>();
        Iterator<String> names = headerNode.getFieldNames();
        while (names.hasNext()) {
            results.add(names.next());
        }
        return results;
    }

    public List<String> fieldsNamed(String name) {
        List<String> results = new ArrayList<String>();
        final Iterator<Map.Entry<String, JsonNode>> items = headerNode.getFields();
        while (items.hasNext()) {
            Map.Entry<String, JsonNode> field = items.next();
            if (field.getKey().equals(name)) {
                results.add(field.getValue().getTextValue());
            }
        }
        return results;
    }
}

//helpers
class RepresentationFactory {
    static Representation createRepresentation(final JsonNode node, final Loader loader, final String contentType) {

        if (node.path("representation").isMissingNode() && node.path("representation-ref").isMissingNode()) {
            return null;
        }

        return new Representation() {
            public String contentType() {
                return contentType;
            }

            public InputStream data() {

                JsonNode representation = node.path("representation");
                if (!representation.isMissingNode()) {
                    return IOUtils.toInputStream(representation.getValueAsText());
                }
                JsonNode repRef = node.path("representation-ref");
                if (!repRef.isMissingNode()) {
                    String resourcePath = repRef.getValueAsText();
                    return loader.load(resourcePath);
                }
                throw new IllegalStateException("rest spec does not have representation or representation-ref response node");
            }

            public String asText() {
                try {
                    String textRepresentation = IOUtils.toString(data());
                    textRepresentation = textRepresentation.replaceAll("\n\\$", "");        //remove newline from last line of response spec
                    return textRepresentation;
                } catch (IOException ioe) {  /*can't do that*/ }
                return null;
            }
        };
    }
}

class RequestFromRestSpec implements Request {
    private final JsonNode requestNode;
    private final Loader loader;
    private final Header theHeader;

    RequestFromRestSpec(JsonNode root, Loader loader) {
        this.requestNode = root.path("request");
        if (this.requestNode.isMissingNode()) {
            throw new RuntimeException("Spec is missing a 'request'");
        }

        this.loader = loader;
        this.theHeader = new HeaderImpl(requestNode.path("header"));
    }

    public String method() {
        JsonNode method = requestNode.path("method");
        if (method.isMissingNode()) {
            throw new RuntimeException("Spec is missing a 'request.method'");
        }
        return method.getValueAsText();
    }

    public Representation representation() {
        return RepresentationFactory.createRepresentation(requestNode, loader, "");
    }

    public Header header() {
        return theHeader;
    }
}

class ResponseFromRestSpec implements Response {
    private final Loader loader;
    private final JsonNode responseNode;
    private final Header theHeader;

    public ResponseFromRestSpec(JsonNode responseNode, Loader loader) {
        this.loader = loader;
        this.responseNode = responseNode;
        this.theHeader = new HeaderImpl(responseNode.path("header"));
    }

    public int statusCode() {
        return responseNode.path("statusCode").getIntValue();
    }

    private <T> T nthOrElse(int n, T defaultValue, List<T> list) {
        if (list.size() > 0) {
            return list.get(n);
        } else {
            return defaultValue;
        }
    }

    public Representation representation() {
        if (responseNode.path("representation").isMissingNode() && responseNode.path("representation-ref").isMissingNode()) {
            return null;
        }

        String contentType = nthOrElse(0, "", theHeader.fieldsNamed("Content-Type"));

        return RepresentationFactory.createRepresentation(responseNode, loader, contentType);
    }

    public Header header() {
        return theHeader;
    }
}