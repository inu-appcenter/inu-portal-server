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

    /*
    모든 이미지의 확장자를 알 수 없는 상태이고 엔티티에서 저장된 이미지의 경로는 모르는 상태이다.
    엔티티에 저장된 이미지의 갯수로, 이미지를 가져올 때 1-1, 1-2, 1-3 의 방식으로 호출한다.
    하지만 정적 이미지를 가져오는 방식에서는 파일의 확장자를 알아야만 가져올 수 있다.
    해당 메소드를 통해 요청으로 들어온 uri 을 기준으로 맞는 파일을 검색한 후 해당 파일의 확장자를 uri 에 추가하여 해당 리소스에 대한 제공을 한다.
     */
    @Override
    public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {
        try {
            // 요청된 경로를 기준으로 실제 파일 경로 계산
            Path requestedPath = Paths.get(path, requestPath);

            // 디렉토리 내에서 파일 검색
            File baseDir = requestedPath.getParent().toFile();
            File[] matchingFiles = baseDir.listFiles((dir, name) -> name.startsWith(requestedPath.getFileName().toString()));

            if (matchingFiles != null && matchingFiles.length > 0) {
                return new UrlResource(matchingFiles[0].toURI());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 파일이 없으면 기본 체인으로 위임
        return chain.resolveResource(request, requestPath, locations);
    }

    @Override
    public String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain) {
        return chain.resolveUrlPath(resourcePath, locations);
    }
}