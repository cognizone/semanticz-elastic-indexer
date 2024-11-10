package zone.cogni.semanticz.webflux;

import io.netty.handler.logging.LogLevel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.net.ssl.SSLException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.StringDecoder;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

public class WebUtils {

    private static final Logger log = LoggerFactory.getLogger(WebUtils.class);

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Request: \n");
                sb.append(clientRequest.method()).append(" ").append(clientRequest.url()).append("\n")
                        .append(clientRequest.cookies());
                clientRequest
                        .headers()
                        .forEach((name, values) -> sb.append(name).append(": ").append(
                                String.join("<SEP>", values)));
                log.debug(sb.toString());
            }
            return Mono.just(clientRequest);
        });
    }

    private static ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (log.isDebugEnabled()) {
                StringBuilder sb = new StringBuilder("Response: \n");
                sb.append(response.statusCode()).append(response.cookies());
                response
                        .headers()
                        .asHttpHeaders()
                        .forEach((name, values) -> sb.append(name).append(": ").append(
                                String.join("<SEP>", values)));
                log.debug(sb.toString());
            }
            return Mono.just(response);
        });
    }

    private static HttpClient createWebClient(boolean followRedirects) {
        return HttpClient
                .create()
                .followRedirect(followRedirects)
                .proxyWithSystemProperties()
                .wiretap(HttpClient.class.getCanonicalName(), LogLevel.TRACE, AdvancedByteBufFormat.TEXTUAL,
                        StandardCharsets.UTF_8);
    }

    public static WebClient createWebClient(boolean restProxy, String username, String password)
            throws SSLException {
        final WebClient.Builder webClient = WebClient.builder()
                .filters(exchangeFilterFunctions -> {
                    exchangeFilterFunctions.add(logRequest());
                    exchangeFilterFunctions.add(logResponse());
                })
                .codecs(configurer -> {
                    configurer.defaultCodecs().maxInMemorySize(16 * 1024 * 1024);
                    StringDecoder decoder = StringDecoder.allMimeTypes();
                    decoder.setDefaultCharset(Charset.defaultCharset());
                    configurer.customCodecs().registerWithDefaultConfig(decoder);
                })
                .clientConnector(new ReactorClientHttpConnector(createWebClient(true)))
                .exchangeStrategies(ExchangeStrategies.builder().codecs(c ->
                        c.defaultCodecs().enableLoggingRequestDetails(true)).build());

        if (restProxy) {
            if (StringUtils.isNoneBlank(username, password)) {
                webClient.defaultHeaders(header -> header.setBasicAuth(username, password));
            }

            final SslContext sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE).build();

            webClient.clientConnector(new ReactorClientHttpConnector(
                    HttpClient.create().secure(t -> t.sslContext(sslContext))));
        }

        return webClient.build();
    }
}

