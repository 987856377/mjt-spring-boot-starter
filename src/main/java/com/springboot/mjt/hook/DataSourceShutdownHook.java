package com.springboot.mjt.hook;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
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
        String[] beanNamesForType = this.applicationContext.getBeanNamesForType(DataSource.class);

        List<DataSource> dataSources = new ArrayList<>();
        for (String beanName : beanNamesForType) {
            Object bean = this.applicationContext.getBean(beanName);
            dataSources.add((DataSource) bean);
        }

        if (dataSources.size() > 0) {
            Runtime.getRuntime().addShutdownHook(new Thread("DataSource shutdown-hook") {
                @Override
                public void run() {
                    dataSources.forEach(dataSource -> {
                        logger.info("{} Shutdown initiated...", dataSource);
                        Class<? extends DataSource> clazz = dataSource.getClass();
                        try {
                            Method closeMethod = clazz.getDeclaredMethod("close");
                            closeMethod.invoke(dataSource);
                            logger.info("{} close completed.", dataSource);
                        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                            logger.info("{} close failed", dataSource);
                            try {
                                Method closeMethod = clazz.getDeclaredMethod("destroy");
                                closeMethod.invoke(dataSource);
                                logger.info("{} destroy completed.", dataSource);
                            } catch (InvocationTargetException | NoSuchMethodException | IllegalAccessException invocationTargetException) {
                                logger.info("{} destroy  failed", dataSource);
                            }
                        }
                    });
                }
            });
        }
    }
}
