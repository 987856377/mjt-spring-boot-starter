package com.springboot.mjt.proxy;

import com.springboot.mjt.factory.DataSourceFactory;
import com.springboot.mjt.factory.MappingJdbcTemplateFactory;
import com.springboot.mjt.selector.EnableMappingJdbcTemplateSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.*;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.proxy
 * @Author xuzhenkui
 * @Date 2021/9/14 12:51
 */
public class JdbcOperationsProxy {
    private static final Logger logger = LoggerFactory.getLogger(JdbcOperationsProxy.class);
    private static final ConcurrentHashMap<String, JdbcOperations> JDBC_OPERATIONS_MAP = new ConcurrentHashMap<>();

    public static JdbcOperations getProxyInstance(String dsName) {
        return getProxyInstance(dsName, EnableMappingJdbcTemplateSelector.ENABLE_EXCHANGE);
    }

    public static JdbcOperations getProxyInstance(String dsName, Boolean exchange) {
        return getProxyInstance(dsName, exchange, EnableMappingJdbcTemplateSelector.ENABLE_FORMAT);
    }

    public static JdbcOperations getProxyInstance(String dsName, Boolean exchange, Boolean format) {
        String unionKey = dsName + ":" + exchange + ":" + format;
        if (JDBC_OPERATIONS_MAP.get(unionKey) == null) {
            DataSource dataSource = DataSourceFactory.getDataSource(dsName);
            Assert.notNull(dataSource, dsName + " datasource is not exists in DataSourceFactory, " +
                    "or you can use { JdbcOperations getProxyInstance(DataSource dataSource, ...) } after build DataSource by yourself!");

            JDBC_OPERATIONS_MAP.putIfAbsent(unionKey, getProxyInstance(dataSource, exchange, format));
        }
        return JDBC_OPERATIONS_MAP.get(unionKey);
    }

    public static JdbcOperations getProxyInstance(DataSource dataSource) {
        return getProxyInstance(dataSource, EnableMappingJdbcTemplateSelector.ENABLE_EXCHANGE);
    }

    public static JdbcOperations getProxyInstance(DataSource dataSource, Boolean exchange) {
        return getProxyInstance(dataSource, exchange, EnableMappingJdbcTemplateSelector.ENABLE_FORMAT);
    }

    public static JdbcOperations getProxyInstance(DataSource dataSource, Boolean exchange, Boolean format) {
        Assert.notNull(dataSource, "DataSource must not be null");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return getInstance(jdbcTemplate, exchange, format);
    }


    private static JdbcOperations getInstance(JdbcTemplate jdbcTemplate, Boolean exchange, Boolean format) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");

        return (JdbcOperations) Proxy.newProxyInstance(JdbcOperations.class.getClassLoader(), new Class<?>[]{JdbcOperations.class}, (proxy, method, args) -> {
            // exchange sql
            if (exchange) {
                preparedStatementSetter(args);
            }

            // convert sql
            String sql = preparedStatementFormatter(args, format);

            Object result = null;
            try {
                long l = System.currentTimeMillis();
                result = method.invoke(jdbcTemplate, args);

                logger.info("\nJdbcOperations Method: {} \nSQL: {} \nInvoke Cost: {}", method.getName(), sql, (System.currentTimeMillis() - l) + "ms");
            } catch (Exception e) {
                logger.error("\nSQL: {} \nError Message: {}", sql, e.getCause().toString());
            }

            return result;
        });
    }

    private static void preparedStatementSetter(Object[] args) {
        if (args[0] instanceof String) {
            String sql = MappingJdbcTemplateFactory.get((String) args[0]);
            if (StringUtils.hasText(sql)) {
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
            } else if (item instanceof Object[]) {
                Arrays.stream(((Object[]) item)).forEach(data -> {
                    if (data instanceof String) {
                        String value = Matcher.quoteReplacement(String.valueOf(data));
                        sql.updateAndGet(s -> s.replaceFirst("\\?", "'" + value + "'"));
                    } else if (data instanceof Collection) {
                        sql.updateAndGet(s -> s.replaceFirst("\\?", String.valueOf(data).replaceAll("\\[", "").replaceAll("]", "")));
                    } else {
                        sql.updateAndGet(s -> s.replaceFirst("\\?", String.valueOf(data)));
                    }
                });
            } else if (item instanceof PreparedStatementCreator && item instanceof PreparedStatementSetter
                    && item instanceof SqlProvider && item instanceof ParameterDisposer) {
                sql.set(((SqlProvider) item).getSql());
            }
        });

        return sql.accumulateAndGet(";", (s, s2) -> s + s2);
    }
}
