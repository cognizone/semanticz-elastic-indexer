package zone.cogni.semanticz.webflux;

import org.apache.commons.io.IOUtils;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.StringUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;

public class TemplateUtils {

    private static final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();

    public static String toString(InputStreamSource resource) {
        return toString(resource, StandardCharsets.UTF_8.name());
    }

    public static String toString(InputStreamSource resource, String encoding) {
        try (InputStream inputStream = resource.getInputStream()) {
            return IOUtils.toString(inputStream, encoding);
        } catch (IOException e) {
            throw new RuntimeException("Failed to convert resource to string", e);
        }
    }

    public static String process(TemplateEngine templateEngine, String template, Map<String, Object> params) {
        return templateEngine.process(template, new Context(Locale.getDefault(), params));
    }

    public static String process(TemplateEngine templateEngine, Resource template, Map<String, Object> params) {
        return process(templateEngine, toString(template), params);
    }

    public static String processResource(TemplateEngine templateEngine, String path, String extFolder, Map<String, Object> params) {
        Resource template = getResource(path, extFolder);
        return process(templateEngine, template, params);
    }

    public static String processResource(TemplateEngine templateEngine, String paramValue, String paramName, String path, String extFolder) {
        Resource template = getResource(path, extFolder);
        return process(templateEngine, template, Map.of(paramName, paramValue));
    }

    public static String processResource(TemplateEngine templateEngine, String path, String extFolder) {
        Resource template = getResource(path, extFolder);
        return process(templateEngine, template, Map.of());
    }

    public static Resource getResource(String path, String extFolder) {
        // First, try the external folder
        if (StringUtils.hasText(extFolder)) {
            Resource resource = resolver.getResource("file:" + extFolder + "/" + path);
            if (resource.exists()) {
                return resource;
            }
        }

        // Then, try the classpath
        Resource resource = resolver.getResource("classpath:" + path);
        if (resource.exists()) {
            return resource;
        }

        return null;
    }

    public static InputStream loadResourceStream(String path, String extFolder) {
        Resource resource = getResource(path, extFolder);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource stream for: " + path, e);
        }
    }

    public static String loadResource(String path, String extFolder) {
        try (InputStream stream = loadResourceStream(path, extFolder)) {
            if (stream == null) {
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource content for: " + path, e);
        }
    }

}
