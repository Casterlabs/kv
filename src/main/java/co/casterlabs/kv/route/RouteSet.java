package co.casterlabs.kv.route;

import java.io.IOException;

import co.casterlabs.kv.AuthPreprocessor;
import co.casterlabs.kv.KV;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class RouteSet implements EndpointProvider {

    @HttpEndpoint(path = "/:key", preprocessor = AuthPreprocessor.class, allowedMethods = {
            HttpMethod.POST
    })
    public HttpResponse on(HttpSession session, EndpointData<Void> data) {
        String key = data.uriParameters().get("key");

        if (!KV.isValidKey(key)) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "The specified key is invalid.");
        }

        long ttl = -1;
        {
            String ttlStr = session.uri().query.getSingle("ttl");
            if (ttlStr != null) {
                try {
                    ttl = Long.parseLong(ttlStr);
                } catch (NumberFormatException e) {
                    return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "Invalid TTL value.");
                }
            }
        }

        String contentType = null;
        {
            HeaderValue header = session.headers().getSingle("Content-Type");
            if (header != null) {
                contentType = header.raw();
            }
        }

        try {
            KV.put(key, contentType, ttl, session.body().stream());
        } catch (IOException e) {
            FastLogger.logException(e);
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst creating the entry.");
        }

        return HttpResponse.newFixedLengthResponse(StandardHttpStatus.CREATED, "Entry created successfully.");
    }

}
