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

public class RouteDelete implements EndpointProvider {

    @HttpEndpoint(path = "/:key", preprocessor = AuthPreprocessor.class, allowedMethods = {
            HttpMethod.DELETE
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
            entry.delete();
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, "Entry deleted successfully.");
        } catch (IOException e) {
            FastLogger.logException(e);
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst deleting the entry.");
        }
    }

}
