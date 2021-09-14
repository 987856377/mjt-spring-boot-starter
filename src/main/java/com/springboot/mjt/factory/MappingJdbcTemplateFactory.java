package com.springboot.mjt.factory;

import org.springframework.util.Assert;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Project springboot-provider
 * @Package com.springboot.provider.mjt.selector.factory
 * @Author xuzhenkui
 * @Date 2021/9/13 16:44
 */
public class MappingJdbcTemplateFactory {
    private static final ConcurrentHashMap<String, String> MAP = new ConcurrentHashMap<>();

    public static void put(String mapperDotId, String sql) {
        Assert.notNull(mapperDotId, "Mapper.Id must not be null");
        Assert.notNull(sql, "SQL must not be null");
        MAP.putIfAbsent(mapperDotId, sql);
    }

    public static String get(String mapperDotId) {
        String sql = MAP.get(mapperDotId);
        Assert.notNull(sql, "SQL must not be null");
        return sql;
    }
}
