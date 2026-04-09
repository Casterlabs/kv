package co.casterlabs.kv.route;

import java.io.IOException;

import co.casterlabs.kv.AuthPreprocessor;
import co.casterlabs.kv.KV;
import co.casterlabs.kv.KV.KVEntry;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class RouteGet implements EndpointProvider {

    @HttpEndpoint(path = "/:key", preprocessor = AuthPreprocessor.class, allowedMethods = {
            HttpMethod.GET
    })
    public HttpResponse on(HttpSession session, EndpointData<Void> data) {
        String key = data.uriParameters().get("key");

        if (!KV.isValidKey(key)) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.BAD_REQUEST, "The specified key is invalid.");
        }

        KVEntry entry;
        try {
            entry = KV.get(key);
        } catch (IOException e) {
            FastLogger.logException(e);
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst retrieving the entry.");
        }

        if (entry == null) {
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.NOT_FOUND, "The specified key does not exist.");
        }

        try {
            return HttpResponse.newRangedFileResponse(session, StandardHttpStatus.OK, entry.data())
                .mime(entry.contentType())
                .header("X-Expires-At", Long.toString(entry.expiresAt()))
                .header("X-Last-Modified", Long.toString(entry.lastModified()))
                .header("Content-Disposition", String.format("filename=\"%s\"", entry.key));
        } catch (IOException e) {
            FastLogger.logException(e);
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst serving file.");
        }
    }

}
