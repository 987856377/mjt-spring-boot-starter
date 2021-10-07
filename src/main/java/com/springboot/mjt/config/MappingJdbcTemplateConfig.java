package com.springboot.mjt.config;

import com.springboot.mjt.annotation.EnableMappingJdbcTemplate;
import com.springboot.mjt.factory.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.util.Arrays;

/**
 * @Description 使用 @{@link EnableMappingJdbcTemplate} 注解, 自动启用 @{@link MappingJdbcTemplateConfig} 并将其中的配置注册到spring context中
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.config
 * @Author xuzhenkui
 * @Date 2021/9/13 15:26
 */
public class MappingJdbcTemplateConfig {
    private final static Logger logger = LoggerFactory.getLogger(MappingJdbcTemplateConfig.class);

    private final ApplicationContext applicationContext;

    public MappingJdbcTemplateConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        initDataSourceFactory();
    }

    public void initDataSourceFactory() {
        String[] beanNamesForType = this.applicationContext.getBeanNamesForType(DataSource.class);
        logger.info(">>> MJT load from Spring ApplicationContext: " + Arrays.toString(beanNamesForType));

        for (String beanName : beanNamesForType) {
            Object bean = this.applicationContext.getBean(beanName);

            DataSourceFactory.addDataSource(beanName, (DataSource)bean);
        }
    }

}
