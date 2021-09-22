package com.springboot.mjt.proxy;

import com.springboot.mjt.factory.DataSourceFactory;
import com.springboot.mjt.factory.MappingJdbcTemplateFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.reflect.Proxy;
import java.util.Arrays;
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
        if (JDBC_OPERATIONS_MAP.get(dsName) == null) {
            DataSource dataSource = DataSourceFactory.getDataSource(dsName);
            Assert.notNull(dataSource, dsName + " datasource is not exists in DataSourceFactory!");

            JDBC_OPERATIONS_MAP.putIfAbsent(dsName, getProxyInstance(dataSource));
        }
        return JDBC_OPERATIONS_MAP.get(dsName);
    }

    public static JdbcOperations getProxyInstance(DataSource dataSource) {
        Assert.notNull(dataSource, "DataSource must not be null");

        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        return getInstance(jdbcTemplate);
    }


    private static JdbcOperations getInstance(JdbcTemplate jdbcTemplate) {
        Assert.notNull(jdbcTemplate, "JdbcTemplate must not be null");

        return (JdbcOperations) Proxy.newProxyInstance(JdbcOperations.class.getClassLoader(), new Class<?>[]{JdbcOperations.class}, (proxy, method, args) -> {
            // exchange sql
            exchange(args);

            // convert sql
            String sql = format(args);

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

    public static void exchange(Object[] args) {
        if (args[0] instanceof String) {
            args[0] = MappingJdbcTemplateFactory.get((String) args[0]);
        }
    }

    private static String format(Object[] args) {
        AtomicReference<String> sql = new AtomicReference<>("");

        // implant the args to sql
        Arrays.stream(args).forEach(item -> {
            if (item instanceof String) {
                sql.set((String) item);
            } else if (item instanceof Object[]) {
                Arrays.stream(((Object[]) item)).forEach(param -> {
                    sql.updateAndGet(s -> s.replaceFirst("\\?", "'" + Matcher.quoteReplacement(param.toString()) + "'"));
                });
            }
        });

        return sql.accumulateAndGet(";", (s, s2) -> s + s2);
    }
}
