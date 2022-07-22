package com.springboot.mjt.hook;

import com.springboot.mjt.factory.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.util.ReflectionUtils;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.hook
 * @Author xuzhenkui
 * @Date 2021/12/21 16:30
 */
public class DataSourceShutdownHook {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ApplicationContext applicationContext;

    public DataSourceShutdownHook(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
        closeDataSources();
    }

    public void closeDataSources() {
        final String[] beanNamesForType = this.applicationContext.getBeanNamesForType(DataSource.class);

        final List<DataSource> dataSources = new ArrayList<>();
        for (String beanName : beanNamesForType) {
            Object bean = this.applicationContext.getBean(beanName);
            dataSources.add((DataSource) bean);
        }

        if (dataSources.size() > 0) {
            Runtime.getRuntime().addShutdownHook(new Thread("MJT-shutdown-hook") {
                @Override
                public void run() {
                    closeApplicationContextDataSources(dataSources);
                    closeDataSourceFactoryDataSources();
                }
            });
        }
    }

    public void closeApplicationContextDataSources(List<DataSource> dataSources) {
        logger.info(">>> MJT start closing ApplicationContext datasource ....");

        for (DataSource dataSource : dataSources) {
            try {
                Method closeMethod = ReflectionUtils.findMethod(dataSource.getClass(), "close");
                if (closeMethod != null) {
                    closeMethod.invoke(dataSource);
                    logger.info("DataSourceShutdownHook close datasource named [{}] success", dataSource);
                } else {
                    closeMethod = ReflectionUtils.findMethod(dataSource.getClass(), "destroy");
                    if (closeMethod != null) {
                        closeMethod.invoke(dataSource);
                        logger.info("DataSourceShutdownHook destroy datasource named [{}] success", dataSource);
                    } else {
                        logger.warn("DataSourceShutdownHook close or destroy datasource named [{}] failed", dataSource);
                    }
                }
            } catch (Exception e) {
                logger.warn("DataSourceShutdownHook closeDataSource named [{}] failed", dataSource, e);
            }
        }

        logger.info(">>> MJT close ApplicationContext datasource success");
    }

    public void closeDataSourceFactoryDataSources() {
        logger.info(">>> MJT start closing DataSourceFactory datasource ....");

        DataSourceFactory.destroy();

        logger.info(">>> MJT close DataSourceFactory datasource success");
    }
}
