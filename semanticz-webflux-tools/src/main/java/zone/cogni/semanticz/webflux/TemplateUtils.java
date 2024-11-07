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

    public static String process(TemplateEngine templateEngine, String template, Map<String, Object> params) {
        return templateEngine.process(template, new Context(Locale.getDefault(), params));
    }

    public static String process(TemplateEngine templateEngine, Resource template, Map<String, Object> params) {
        return process(templateEngine, toString(template), params);
    }

    public static String toString(InputStreamSource resource) {
        return toString(resource, "UTF-8");
    }

    public static String toString(InputStreamSource resource, String encoding) {
        try {
            InputStream inputStream = resource.getInputStream();

            String var3;
            try {
                var3 = IOUtils.toString(inputStream, encoding);
            } catch (Throwable var6) {
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (Throwable var5) {
                        var6.addSuppressed(var5);
                    }
                }

                throw var6;
            }

            if (inputStream != null) {
                inputStream.close();
            }

            return var3;
        } catch (IOException var7) {
            throw new RuntimeException(var7);
        }
    }

    public static String processResource(TemplateEngine templateEngine, String path, String extFolder, Map<String, Object> params) {
        if(!StringUtils.hasText(path)) {
            return null;
        }
        Resource template = TemplateUtils.getResource(path, extFolder);
        return TemplateUtils.process(
                templateEngine,
                template,
                params);
    }
    public static String processResource(TemplateEngine templateEngine, String paramValue, String paramName, String path, String extFolder) {
        if(!StringUtils.hasText(path)) {
            return null;
        }
        Resource template = TemplateUtils.getResource(path, extFolder);
        return TemplateUtils.process(
                templateEngine,
                template,
                Map.of(paramName, paramValue));
    }

    public static String processResource(TemplateEngine templateEngine, String path, String extFolder) {
        if(!StringUtils.hasText(path)) {
            return null;
        }
        Resource constructTemplate = TemplateUtils.getResource(path, extFolder);
        return TemplateUtils.process(
                templateEngine,
                constructTemplate,
                Map.of());
    }

    public static Resource getResource(String path, String extFolder) {
        if(!StringUtils.hasText(path)) {
            return null;
        }
        // First try classpath
        Resource resource = resolver.getResource("classpath:" + path);
        if (resource.exists()) {
            return resource;
        }

        // If not found, try the external folder
        if (StringUtils.hasText(extFolder)) {
            resource = resolver.getResource("file:" + extFolder + "/" + path);
            if (resource.exists()) {
                return resource;
            }
        }

        return null; // or throw an exception if resource is mandatory
    }
    public static InputStream loadResourceStream(String path, String extFolder) {
        if(!StringUtils.hasText(path)) {
            return null;
        }
        Resource resource = getResource(path, extFolder);
        if (resource == null) {
            throw new RuntimeException("Resource not found: " + path);
        }
        try {
            return resource.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String loadResource(String path, String extFolder) {
        try {
            InputStream stream = loadResourceStream(path, extFolder);
            if(stream == null) {
                return null;
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
