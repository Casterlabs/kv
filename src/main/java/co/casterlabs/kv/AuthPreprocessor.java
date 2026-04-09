package co.casterlabs.kv;

import co.casterlabs.kv.util.EnvHelper;
import co.casterlabs.rhs.HttpStatus.StandardHttpStatus;
import co.casterlabs.rhs.protocol.HeaderValue;
import co.casterlabs.rhs.protocol.api.preprocessors.Preprocessor;
import co.casterlabs.rhs.protocol.http.HttpResponse;
import co.casterlabs.rhs.protocol.http.HttpSession;

public class AuthPreprocessor implements Preprocessor.Http<Void> {
    private static final String AUTH_SECRET = "Bearer " + EnvHelper.string("KV_AUTH_SECRET", "insecure");

    @Override
    public void preprocess(HttpSession session, PreprocessorContext<HttpResponse, Void> context) {
        HeaderValue authHeader = session.headers().getSingle("Authorization");

        if (authHeader == null || !authHeader.raw().equals(AUTH_SECRET)) {
            context.respondEarly(
                HttpResponse.newFixedLengthResponse(StandardHttpStatus.UNAUTHORIZED, "Unauthorized")
            );
        }
    }

}
