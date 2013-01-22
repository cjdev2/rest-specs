/*jslint newcap: false*/
/*global $, RestSpecUtil:true, RegExp, RestSpec */

function RestSpecUtil(restSpecs, options) {
    var getRepresentationRefText, resolveRepresentation, replaceUrl, getSpec, client, pathPrefix;

    if (typeof options !== "undefined") {
        client = options.client;
        pathPrefix = options.pathPrefix;
    }

    if (typeof restSpecs === "undefined") {
        restSpecs = RestSpec;
    }

    if (typeof client === "undefined") {
        client = $;
    }

    if (typeof pathPrefix === "undefined") {
        pathPrefix = "";
    }

    replaceUrl = function (spec, urlReplacements) {
        if (typeof urlReplacements === 'undefined') {
            return spec;
        }

        $.each(urlReplacements, function (key, value) {
            if (typeof spec.url !== "undefined") {
                spec.url = spec.url.replace(new RegExp(key, "g"), value);
            }
        });

        return spec;
    };


    resolveRepresentation = function (target) {
        var updatedRep = {}, xmlRepresentation, jsonRepresentation, xmlError, jsonError, responseRepresentationisXml;

        if (typeof target !== "undefined" && typeof target["representation-ref"] !== "undefined") {
            target.representation = getRepresentationRefText(target["representation-ref"]);
        }

        if (typeof target === 'undefined' || typeof target.representation === 'undefined') {
            return undefined;
        }

        xmlRepresentation = target.representation;
        try {
            xmlRepresentation = $.parseXML(xmlRepresentation);
        } catch (e) {
            xmlError = e;
        }

        try {
            responseRepresentationisXml = target.header["Content-Type"].indexOf("xml") != -1;
        } catch (e2) {
            responseRepresentationisXml = false;
        }

        if (typeof xmlError !== "undefined" && responseRepresentationisXml) {
            throw "Your response representation may not be well-formed.  Possible cause: an XML marker tag like < or & not wrapped in a CDATA";
        }

        jsonRepresentation = target.representation;
        try {
            jsonRepresentation = $.parseJSON(jsonRepresentation);
        } catch (e3) {
            jsonError = e3;
        }

        updatedRep = {
            asXml: xmlRepresentation,
            asText: target.representation,
            asJson: jsonRepresentation
        };

        target.representation = updatedRep;
        return updatedRep;
    };

    getSpec = function (key, urlReplacements, credentials) {
        var matchedSpec;
        $(restSpecs).each(function (i, spec) {
            if (spec.name === key) {
                matchedSpec = $.extend(true, {}, spec);
                resolveRepresentation(matchedSpec.response);
                resolveRepresentation(matchedSpec.request);

                return false; // stop processing
            }
        });

        if (typeof matchedSpec === 'undefined') {
            return;
        }

        if (typeof credentials !== 'undefined') {
            matchedSpec.request.credentials = credentials;
        }

        return replaceUrl(matchedSpec, urlReplacements);
    };

    getRepresentationRefText = function (referencePath) {
        var theContents;
        client.ajax({
            url: pathPrefix + referencePath,
            success: function (data, textStatus, jqXHR) {
                theContents = jqXHR.responseText;
            },
            async: false
        });
        return theContents;
    };

    return {
        getSpec: getSpec,
        getRepresentationRefText: getRepresentationRefText
    };
}
