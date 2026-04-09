package co.casterlabs.kv;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import co.casterlabs.kv.route.RouteDelete;
import co.casterlabs.kv.route.RouteEnumerate;
import co.casterlabs.kv.route.RouteGet;
import co.casterlabs.kv.route.RouteSet;
import co.casterlabs.kv.util.EnvHelper;
import co.casterlabs.kv.util.RakuraiTaskExecutor;
import co.casterlabs.rhs.HttpServer;
import co.casterlabs.rhs.HttpServerBuilder;
import co.casterlabs.rhs.protocol.api.ApiFramework;
import co.casterlabs.rhs.protocol.http.HttpProtocol;
import co.casterlabs.rhs.protocol.websocket.WebsocketProtocol;
import xyz.e3ndr.fastloggingframework.FastLoggingFramework;

public class Bootstrap {

    public static void main(String[] args) throws IOException, UnrecoverableKeyException, KeyStoreException, NoSuchAlgorithmException, CertificateException {
        System.setProperty("fastloggingframework.wrapsystem", "true");
        FastLoggingFramework.setColorEnabled(false);

        ApiFramework framework = new ApiFramework();
        framework.register(new RouteDelete());
        framework.register(new RouteEnumerate());
        framework.register(new RouteGet());
        framework.register(new RouteSet());

        final int PORT = EnvHelper.integer("KV_PORT", 8080);

        KV.init();

        HttpServer server = new HttpServerBuilder()
            .withPort(PORT)
            .withBehindProxy(true)
            .withKeepAliveSeconds(120)
            .withMinSoTimeoutSeconds(120)
            .withServerHeader("casterlabs-kv/1")
            .withTaskExecutor(RakuraiTaskExecutor.INSTANCE)
            .with(new HttpProtocol(), framework.httpHandler)
            .with(new WebsocketProtocol(), framework.websocketHandler)
            .build();

        server.start();
    }

}
