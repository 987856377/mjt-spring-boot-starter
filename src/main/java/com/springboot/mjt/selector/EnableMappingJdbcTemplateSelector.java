package com.springboot.mjt.selector;

import com.springboot.mjt.annotation.EnableMappingJdbcTemplate;
import com.springboot.mjt.config.MappingJdbcTemplateConfig;
import com.springboot.mjt.factory.MappingJdbcTemplateFactory;
import com.springboot.mjt.task.XMLResourceRecursiveTask;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.stream.Stream;

/**
 * @Description 使用 @{@link EnableMappingJdbcTemplate} 注解, 自动启用 @{@link MappingJdbcTemplateConfig} 并将其中的配置注册到spring context中
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.selector
 * @Author xuzhenkui
 * @Date 2021/9/13 15:24
 */
public class EnableMappingJdbcTemplateSelector implements ImportSelector {

    private static final String MAPPING_JDBC_TEMPLATE_CONFIG = "com.springboot.mjt.config.MappingJdbcTemplateConfig";
    private static final String DATASOURCE_SHUTDOWN_HOOK = "com.springboot.mjt.hook.DataSourceShutdownHook";

    private static final String BASE_LOCATIONS = "baseLocations";

    private String[] mapperLocations = new String[]{"classpath*:/mapper/**/*.xml", "classpath*:/xml/**/*.xml", "classpath*:/mjt/**/*.xml"};

    private static final ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();

    private static final MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory();

    /**
     * Select and return the names of which class(es) should be imported based on
     * the {@link AnnotationMetadata} of the importing @{@link Configuration} class.
     *
     * @param importingClassMetadata
     * @return the class names, or an empty array if none
     */
    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableMappingJdbcTemplate.class.getName());

        Object[] baseLocations = (Object[]) Optional.ofNullable(annotationAttributes).map(map -> map.get(BASE_LOCATIONS)).orElseGet(() -> new Object[]{});

        Resource[] resources = resolveMapperLocations((String[]) baseLocations);

        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors() / 2 + 1);

        ForkJoinTask<Map<String, String>> xmlMappingMap = pool.submit(new XMLResourceRecursiveTask(resources));
        try {
            MappingJdbcTemplateFactory.putAll(xmlMappingMap.get());
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        } finally {
            pool.shutdown();
        }

        return new String[]{MAPPING_JDBC_TEMPLATE_CONFIG, DATASOURCE_SHUTDOWN_HOOK};
    }

    public Resource[] resolveMapperLocations(String[] baseLocations) {
        return Stream.of(Optional.ofNullable(baseLocations).orElse(this.mapperLocations))
                .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
    }

    private Resource[] getResources(String location) {
        try {
            return resourcePatternResolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }
}
