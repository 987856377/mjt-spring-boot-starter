package com.springboot.mjt.loader;

import org.dom4j.Element;

import java.util.List;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.loader
 * @Author xuzhenkui
 * @Date 2021/9/16 10:16
 */
public class ElementMapping {
    private String mapper;

    private List<Element> sqlList;

    public ElementMapping() {
    }

    public ElementMapping(String mapper, List<Element> sqlList) {
        this.mapper = mapper;
        this.sqlList = sqlList;
    }

    public String getMapper() {
        return mapper;
    }

    public void setMapper(String mapper) {
        this.mapper = mapper;
    }

    public List<Element> getSqlList() {
        return sqlList;
    }

    public void setSqlList(List<Element> sqlList) {
        this.sqlList = sqlList;
    }

    @Override
    public String toString() {
        return "ElementMapping{" +
                "mapper='" + mapper + '\'' +
                ", sqlList=" + sqlList +
                '}';
    }
}
