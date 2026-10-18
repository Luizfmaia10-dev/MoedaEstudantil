package moedaEstudantil.config;

import moedaEstudantil.service.ArquivoStorageService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serve as imagens enviadas em /uploads/**. */
@Configuration
public class UploadWebConfig implements WebMvcConfigurer {

    private final ArquivoStorageService storage;

    public UploadWebConfig(ArquivoStorageService storage) {
        this.storage = storage;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(ArquivoStorageService.URL_PREFIX + "**")
                .addResourceLocations(storage.getDiretorio().toUri().toString());
    }
}
