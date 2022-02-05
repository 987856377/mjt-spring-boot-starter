package com.springboot.mjt.factory;

import org.springframework.util.StringUtils;

import javax.sql.DataSource;
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
    public static Boolean addDataSource(String dsName, DataSource dataSource) {
        if (StringUtils.hasText(dsName) && dataSource != null) {
            return DATA_SOURCE_MAP.putIfAbsent(dsName, dataSource) == null;
        }
        return false;
    }

    /**
     * 删除数据源
     *
     * @param dsName 数据源名称
     */
    public static Boolean removeDataSource(String dsName) {
        if (StringUtils.hasText(dsName)) {
            DATA_SOURCE_MAP.remove(dsName);
            return true;
        }
        return false;
    }

}
