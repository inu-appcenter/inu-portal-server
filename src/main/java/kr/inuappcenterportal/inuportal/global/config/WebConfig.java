package kr.inuappcenterportal.inuportal.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration

public class WebConfig implements WebMvcConfigurer {
    private final CustomResourceResolver customResourceHttpRequestHandler;
    public WebConfig(CustomResourceResolver customResourceHttpRequestHandler) {
        this.customResourceHttpRequestHandler = customResourceHttpRequestHandler;
    }

    @Value("${imagePath}")
    private String path;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:"+path)
                .setCachePeriod(3600)
                .resourceChain(true)
                .addResolver(customResourceHttpRequestHandler);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/images/**")
                .allowedOrigins("*")
                .allowedMethods("*");
    }
}
