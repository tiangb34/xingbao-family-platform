package cn.xingbao.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  @Override public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry.addResourceHandler("/admin/**").addResourceLocations("file:./admin-web/");
    registry.addResourceHandler("/app/**").addResourceLocations("file:./mobile-web/");
    registry.addResourceHandler("/uploads/**").addResourceLocations("file:./uploads/");
  }
  @Override public void addCorsMappings(CorsRegistry registry) {
    registry.addMapping("/api/**").allowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*").allowedMethods("GET","POST","PUT","DELETE","OPTIONS").allowedHeaders("*");
  }
}
