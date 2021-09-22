package com.springboot.mjt.factory;

import org.springframework.util.Assert;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.factory
 * @Author xuzhenkui
 * @Date 2021/9/13 16:44
 */
public class MappingJdbcTemplateFactory {
    private static final Map<String, String> MAP = new ConcurrentHashMap<>();

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

    public static void putAll(Map<String, String> map) {
        Assert.notNull(map, "Map must not be null");
        MAP.putAll(map);
    }
}
