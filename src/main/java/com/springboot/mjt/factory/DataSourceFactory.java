package com.springboot.mjt.factory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description 多数据源控制器
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.factory
 * @Author xuzhenkui
 * @Date 2020/2/26 18:27
 */
public class DataSourceFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

    private static final Map<String, DataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    /**
     * 根据数据源名称获取数据源
     *
     * @param dsName 数据源名称
     * @return
     */
    public static DataSource getDataSource(String dsName) {
        if (StringUtils.hasText(dsName)) {
            return DATA_SOURCE_MAP.get(dsName);
        }
        return null;
    }

    /**
     * 添加数据源
     *
     * @param dsName     数据源名称
     * @param dataSource 数据源
     */
    public static synchronized void addDataSource(String dsName, DataSource dataSource) {
        DataSource oldDataSource = DATA_SOURCE_MAP.put(dsName, dataSource);
        // 关闭老的数据源
        if (oldDataSource != null) {
            closeDataSource(oldDataSource);
            LOGGER.info("DataSourceFactory close old datasource named [{}] success", dsName);
        }
        LOGGER.info("DataSourceFactory - add datasource named [{}] success", dsName);
    }

    /**
     * 删除数据源
     *
     * @param dsName 数据源名称
     */
    public static void removeDataSource(String dsName) {
        if (!StringUtils.hasText(dsName)) {
            throw new RuntimeException("remove parameter could not be empty");
        }
        if (DATA_SOURCE_MAP.containsKey(dsName)) {
            DataSource dataSource = DATA_SOURCE_MAP.remove(dsName);
            closeDataSource(dataSource);
            LOGGER.info("DataSourceFactory - remove the database named [{}] success", dsName);
        } else {
            LOGGER.warn("DataSourceFactory - could not find a database named [{}]", dsName);
        }
    }

    private static void closeDataSource(DataSource dataSource) {
        try {
            Method closeMethod = ReflectionUtils.findMethod(dataSource.getClass(), "close");
            if (closeMethod != null) {
                closeMethod.invoke(dataSource);
                LOGGER.info("DataSourceFactory close datasource named [{}] success", dataSource);
            } else {
                closeMethod = ReflectionUtils.findMethod(dataSource.getClass(), "destroy");
                if (closeMethod != null) {
                    closeMethod.invoke(dataSource);
                    LOGGER.info("DataSourceFactory destroy datasource named [{}] success", dataSource);
                } else {
                    LOGGER.warn("DataSourceFactory close or destroy datasource named [{}] failed", dataSource);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("DataSourceFactory closeDataSource named [{}] failed", dataSource, e);
        }
    }

    public static void destroy() {
        LOGGER.info("DataSourceFactory start closing ....");

        DATA_SOURCE_MAP.values().forEach(DataSourceFactory::closeDataSource);

        LOGGER.info("DataSourceFactory all closed success,bye");
    }

}
