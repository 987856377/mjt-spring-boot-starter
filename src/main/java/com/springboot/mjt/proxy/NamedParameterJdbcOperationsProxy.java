package com.springboot.mjt.proxy;

import com.springboot.mjt.factory.DataSourceFactory;
import com.springboot.mjt.factory.MappingJdbcTemplateFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

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

        NamedParameterJdbcTemplate namedParameterJdbcTemplate = new NamedParameterJdbcTemplate(dataSource);

        return getInstance(namedParameterJdbcTemplate, exchange, format);
    }

    private static NamedParameterJdbcOperations getInstance(NamedParameterJdbcTemplate namedParameterJdbcTemplate, Boolean exchange, Boolean format) {
        Assert.notNull(namedParameterJdbcTemplate, "NamedParameterJdbcTemplate must not be null");

        return (NamedParameterJdbcOperations) Proxy.newProxyInstance(NamedParameterJdbcOperations.class.getClassLoader(), new Class<?>[]{NamedParameterJdbcOperations.class}, (proxy, method, args) -> {
            // exchange sql
            if (exchange) {
                preparedStatementSetter(args);
            }

            // format sql
            String sql = preparedStatementFormatter(args, format);

            Object result = null;
            try {
                long l = System.currentTimeMillis();
                result = method.invoke(namedParameterJdbcTemplate, args);

                logger.info("\nNamedParameterJdbcOperations Method: {} \nSQL: {} \nInvoke Cost: {}", method.getName(), sql, (System.currentTimeMillis() - l) + "ms");
            } catch (Exception e) {
                logger.error("\nSQL: {} \nError Message: {}", sql, e.getCause().toString());
            }

            return result;
        });
    }

    private static void preparedStatementSetter(Object[] args) {
        if (args[0] instanceof String) {
            String sql = MappingJdbcTemplateFactory.get((String) args[0]);
            if (StringUtils.isNotBlank(sql)) {
                args[0] = sql;
            }
        }
    }

    private static String preparedStatementFormatter(Object[] args, Boolean format) {
        if (!format && args[0] instanceof String) {
            return (String) args[0];
        }

        AtomicReference<String> sql = new AtomicReference<>("");

        // implant the args to sql
        Arrays.stream(args).forEach(item -> {
            if (item instanceof String) {
                sql.set((String) item);
            } else if (item instanceof PreparedStatementCreator && item instanceof PreparedStatementSetter
                    && item instanceof SqlProvider && item instanceof ParameterDisposer) {

                sql.set(((SqlProvider) item).getSql());
            } else if (item instanceof Map) {
                Map<String, Object> map = (Map<String, Object>) item;

                Set<String> keys = map.keySet();
                keys.forEach(key -> {
                    sqlParameterSetter(sql, key, map.get(key));
                });
            } else if (item instanceof SqlParameterSource) {
                SqlParameterSource sqlParameterSource = (SqlParameterSource) item;

                String[] parameterNames = sqlParameterSource.getParameterNames();
                if (parameterNames != null && parameterNames.length > 0) {
                    Arrays.stream(parameterNames).forEach(key -> {
                        sqlParameterSetter(sql, key, sqlParameterSource.getValue(key));
                    });
                }
            }
        });

        return sql.accumulateAndGet(";", (s, s2) -> s + s2);
    }

    private static void sqlParameterSetter(AtomicReference<String> sql, String key, Object data) {
        String source = StringUtils.leftPad(key, key.length() + 1, ':');
        if (sql.get().contains(source)) {
            String value = String.valueOf(data);
            if (StringUtils.isNotEmpty(value)) {
                sql.updateAndGet(s -> s.replaceAll(source, Matcher.quoteReplacement(StringUtils.center(value, value.length() + 2, "'"))));
            }
        }
    }

}
