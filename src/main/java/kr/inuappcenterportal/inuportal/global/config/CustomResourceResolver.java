package kr.inuappcenterportal.inuportal.global.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceResolver;
import org.springframework.web.servlet.resource.ResourceResolverChain;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Component
public class CustomResourceResolver implements ResourceResolver {

    @Value("${imagePath}")
    private String path;

    @Override
    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        try {
            String path = requestPath + ".webp";
            Path imagePath = Paths.get(path, path);

            return new UrlResource(imagePath.toUri());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 기본 체인으로 위임
        return chain.resolveResource(request, requestPath, locations);
    }

    @Override
    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
        return chain.resolveUrlPath(resourcePath, locations);
    }
}