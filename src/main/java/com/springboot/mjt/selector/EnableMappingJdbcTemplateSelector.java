package com.springboot.mjt.selector;

import com.springboot.mjt.annotation.EnableMappingJdbcTemplate;
import com.springboot.mjt.config.MappingJdbcTemplateConfig;
import com.springboot.mjt.factory.MappingJdbcTemplateFactory;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @Description 使用 @{@link EnableMappingJdbcTemplate} 注解, 自动启用 @{@link MappingJdbcTemplateConfig} 并将其中的配置注册到spring context中
 * @Project springboot-provider
 * @Package com.springboot.provider.common.selector
 * @Author xuzhenkui
 * @Date 2021/9/13 15:24
 */
public class EnableMappingJdbcTemplateSelector implements ImportSelector {

    private static final String MAPPING_JDBC_TEMPLATE_CONFIG = "com.springboot.mjt.config.MappingJdbcTemplateConfig";

    private static final String BASE_LOCATIONS = "baseLocations";

    private String[] mapperLocations = new String[]{"classpath*:/mapper/**/*.xml", "classpath*:/xml/**/*.xml", "classpath*:/mjt/**/*.xml"};

    private static final ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

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
        assert annotationAttributes != null;
        Object[] baseLocations = (Object[]) annotationAttributes.get(BASE_LOCATIONS);
        if (baseLocations != null && baseLocations.length > 0) {
            this.mapperLocations = (String[]) baseLocations;
        }

        Resource[] resources = resolveMapperLocations();

        Arrays.stream(resources).forEach(resource -> {
            SAXReader reader = new SAXReader();
            try {
                Document document = reader.read(resource.getInputStream());
                Element namespace = document.getRootElement();
                String mapper = namespace.attributeValue("mapper");
                List<Element> sqls = namespace.elements("sql");

                sqls.forEach(sql -> MappingJdbcTemplateFactory.put(mapper + "." + sql.attributeValue("id"), sql.getTextTrim()));

            } catch (DocumentException | IOException e) {
                e.printStackTrace();
            }
        });

        return new String[]{MAPPING_JDBC_TEMPLATE_CONFIG};
    }

    public Resource[] resolveMapperLocations() {
        return Stream.of(Optional.ofNullable(this.mapperLocations).orElse(new String[0]))
                .flatMap(location -> Stream.of(getResources(location))).toArray(Resource[]::new);
    }

    private Resource[] getResources(String location) {
        try {
            return resourceResolver.getResources(location);
        } catch (IOException e) {
            return new Resource[0];
        }
    }
}
