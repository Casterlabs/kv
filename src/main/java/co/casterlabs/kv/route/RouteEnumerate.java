package co.casterlabs.kv.route;

import java.io.IOException;

import co.casterlabs.kv.AuthPreprocessor;
import co.casterlabs.kv.KV;
import co.casterlabs.kv.KV.KVEntry;
import co.casterlabs.rakurai.json.element.JsonArray;
import co.casterlabs.rhs.HttpMethod;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointData;
import co.casterlabs.rhs.protocol.api.endpoints.EndpointProvider;
import co.casterlabs.rhs.protocol.api.endpoints.HttpEndpoint;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;
import xyz.e3ndr.fastloggingframework.logging.FastLogger;

public class RouteEnumerate implements EndpointProvider {

    @HttpEndpoint(path = "/", preprocessor = AuthPreprocessor.class, allowedMethods = {
            HttpMethod.GET
    })
    public HttpResponse on(HttpSession session, EndpointData<Void> data) {
        JsonArray arr = new JsonArray();

        try {
            for (KVEntry entry : KV.enumerate()) {
                arr.add(entry.toJson());
            }

            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.OK, arr.toString())
                .mime("application/json; charset=utf-8");
        } catch (IOException e) {
            FastLogger.logException(e);
            return HttpResponse.newFixedLengthResponse(StandardHttpStatus.INTERNAL_ERROR, "An error occurred whilst enumerating entries.");
        }
    }

}
