# WebFlux Proxy and Template Utilities Library

A Java library providing tools for working with proxy calls using Spring WebFlux and Thymeleaf templates.

## Features

- Simplify the creation of proxy endpoints with WebFlux.
- Process templates using Thymeleaf.
- Utility methods for resource loading and processing.

## Installation

Add the following dependencies to your project:

```gradle
dependencies {
    implementation("org.springframework:spring-web:5.3.30")
    implementation("org.springframework:spring-context:5.3.30")
    implementation("org.springframework:spring-webflux:5.3.30")
    implementation("org.thymeleaf:thymeleaf:3.0.15.RELEASE")
    // Other dependencies...
}
```

## Usage

### Creating a Proxy Endpoint

Extend the `WebProxy` class to create a custom proxy:

```java
@Service
public class ElasticProxy extends WebProxy {

    public ElasticProxy(@Value("${datacat.elasticsearch.url}") String url,
                        @Value("${datacat.elasticsearch.user:}") String username,
                        @Value("${datacat.elasticsearch.password:}") String password,
                        @Value("${datacat.elasticsearch.proxyPath:}") String indexName,
                        @Value("${datacat.elasticsearch.readtimeout:#{null}}") Integer readTimeout,
                        @Value("${datacat.elasticsearch.connectTimeout:#{null}}") Integer connectTimeout) throws SSLException {
        super(url, username, password, indexName, readTimeout, connectTimeout);
    }
}
```

Use the proxy in your controller:

```java
@PostMapping("/api/elastic/**")
public String filter(
        HttpServletRequest request,
        @RequestBody(required = false) String body,
        @RequestHeader(value = "Accept", required = false) String accept,
        @RequestHeader(value = "Content-Type", required = false) String contentType) {
    return elasticProxy.proxy(
            StringUtils.substringAfter(request.getRequestURI(), "/api/elastic"),
            HttpMethod.POST,
            accept, contentType,
            body);
}
```

### Processing Templates

Utilize `TemplateUtils` for template processing:

```java
String result = TemplateUtils.process(templateEngine, "templateName", params);
```

## License

This project is licensed under the Apache-2.0 License. You may obtain a copy of the License at:

- **[LICENSE](./../LICENSE)**