package zone.cogni.semanticz.webflux;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class WebProxy {

    private static final Logger log = LoggerFactory.getLogger(WebProxy.class);

    private String url;
    private String username;
    private String password;
    private String endpoint;
    private Integer readTimeout;
    private Integer connectTimeout;

    private WebClient webClient;

    public WebProxy(String url, String username, String password, String endpoint, Integer readTimeout, Integer connectTimeout) throws SSLException {
        this.url = url;
        this.username = username;
        this.password = password;
        this.endpoint = endpoint;
        this.readTimeout = readTimeout;
        this.connectTimeout = connectTimeout;
        this.webClient = createWebClient();
    }

    public WebClient createWebClient() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
                                                 .trustManager(InsecureTrustManagerFactory.INSTANCE)
                                                 .build();

        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(sslContext));

        if (connectTimeout != null) {
            httpClient.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout);
        }
        if (readTimeout != null) {
            httpClient.doOnConnected(conn -> conn
                    .addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS)));
        }

        webClient = WebUtils
                .createWebClient(true, username, password)
                .mutate()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();

        log.info(
                "Web Client created for {} with username \"{}\"",
                url, username);

        return webClient;
    }

    public String proxy(String requestPath, HttpMethod method, String accept, String contentType, String body) {
        return proxyResponse(requestPath, method, accept, contentType, body).getBody();
    }

    public ResponseEntity<String> proxyResponse(String requestPath, HttpMethod method, String accept, String contentType, String body) {
        final String uri = StringUtils.removeEnd(url, "/") + endpoint + requestPath;

        WebClient.RequestBodySpec request = webClient
                .method(method)
                .uri(URI.create(uri));

        if(accept != null) {
            request = request.headers(httpHeaders -> httpHeaders.set("Accept", accept));
        }
        if(contentType != null) {
            request = request.headers(httpHeaders -> httpHeaders.set("Content-Type", contentType));
        }
        if (body != null) {
            return request.bodyValue(body)
                          .exchangeToMono(response -> processResponse(response))
                          .block();
        }

        return request
                .exchangeToMono(response -> processResponse(response))
                .block();
    }

    private Mono<ResponseEntity<String>> processResponse(ClientResponse response) {
        return response.bodyToMono(String.class)
                       .map(body -> ResponseEntity
                               .status(response.rawStatusCode())
                               .headers(response.headers().asHttpHeaders())
                               .body(body));
    }

}
