package com.springboot.mjt.proxy;

import com.springboot.mjt.factory.DataSourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.proxy
 * @Author xuzhenkui
 * @Date 2021/10/20 10:44
 */
public class NamedParameterJdbcOperationsProxy {
    private static final Logger logger = LoggerFactory.getLogger(NamedParameterJdbcOperationsProxy.class);
    private static final ConcurrentHashMap<String, NamedParameterJdbcOperations> NAMED_PARAMETER_JDBC_OPERATIONS_MAP = new ConcurrentHashMap<>();

    public static NamedParameterJdbcOperations getProxyInstance(String dsName) {
        return getProxyInstance(dsName, true);
    }

    public static NamedParameterJdbcOperations getProxyInstance(String dsName, Boolean exchange) {
        if (NAMED_PARAMETER_JDBC_OPERATIONS_MAP.get(dsName) == null) {
            DataSource dataSource = DataSourceFactory.getDataSource(dsName);
            Assert.notNull(dataSource, dsName + " datasource is not exists in DataSourceFactory!");

            NAMED_PARAMETER_JDBC_OPERATIONS_MAP.putIfAbsent(dsName, getProxyInstance(dataSource, exchange));
        }
        return NAMED_PARAMETER_JDBC_OPERATIONS_MAP.get(dsName);
    }

    public static NamedParameterJdbcOperations getProxyInstance(DataSource dataSource) {
        return getProxyInstance(dataSource, true);
    }

    public static NamedParameterJdbcOperations getProxyInstance(DataSource dataSource, Boolean exchange) {
        return getProxyInstance(dataSource, exchange, true);
    }

    public static NamedParameterJdbcOperations getProxyInstance(DataSource dataSource, Boolean exchange, Boolean format) {
        Assert.notNull(dataSource, "DataSource must not be null");

        return new NamedParameterJdbcTemplate(JdbcOperationsProxy.getProxyInstance(dataSource, exchange, format));
    }

}
