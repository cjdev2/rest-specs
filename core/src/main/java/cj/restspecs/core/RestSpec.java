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

    private RestSpec(RestSpec originalSpec, Map<String, Object> replacements) {
        this.root = originalSpec.root;
        this.name = originalSpec.name;
        this.url = originalSpec.url;
        this.loader = originalSpec.loader;

        this.replacements = new HashMap<String, Object>();
        this.replacements.putAll(originalSpec.replacements);
        this.replacements.putAll(replacements);
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

            name = root.path("name").getValueAsText();
            url = root.path("url").getValueAsText();

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
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        int delimiterPos = url.indexOf('?');
        if (delimiterPos == -1) {
            return new String[]{url, ""};
        } else {
            return new String[]{
                    url.substring(0, delimiterPos),
                    url.substring(delimiterPos)
            };
        }
    }

    public Map<String, String> queryParams() {
        try {

            Map<String, String> queryParams = new HashMap<String, String>();
            String query = queryString();
            if (query != "") {
                for (String param : query.substring(1).split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], "UTF-8");
                    String value = null;
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], "UTF-8");
                    }
                    queryParams.put(key, value);
                }
            }

            return queryParams;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
        return new Request() {

            final JsonNode requestNode = root.path("request");
            final Header theHeader = new HeaderImpl(requestNode.path("header"));

            public String method() {
                return root.path("request").path("method").getValueAsText();
            }

            public Representation representation() {
                return RepresentationFactory.createRepresentation(requestNode, loader, "");
            }

            public Header header() {
                return theHeader;
            }
        };
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