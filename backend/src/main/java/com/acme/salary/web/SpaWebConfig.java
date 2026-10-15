package com.acme.salary.web;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the Angular build (copied into {@code classpath:/static} at packaging time) from the same
 * origin as the API. Real files are served as-is; any other non-API path returns {@code index.html}
 * so deep links such as {@code /employees/7/edit} survive a browser refresh. Unknown {@code /api}
 * paths still get a 404 instead of an HTML page.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
				.addResourceLocations("classpath:/static/")
				.resourceChain(true)
				.addResolver(new PathResourceResolver() {
					@Override
					protected Resource getResource(String resourcePath, Resource location) throws IOException {
						Resource requested = location.createRelative(resourcePath);
						if (requested.exists() && requested.isReadable()) {
							return requested;
						}
						if (resourcePath.startsWith("api/") || resourcePath.startsWith("actuator/")) {
							return null;
						}
						Resource index = new ClassPathResource("/static/index.html");
						return index.exists() ? index : null;
					}
				});
	}

}
