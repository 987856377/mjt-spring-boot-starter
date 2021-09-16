package com.springboot.mjt.loader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.Resource;

import java.io.IOException;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.loader
 * @Author xuzhenkui
 * @Date 2021/9/16 10:16
 */
public class ResourceLoader {

    private static final String MAPPER = "mapper";

    private static final String SQL = "sql";

    public static ElementMapping load(Resource resource) {
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(resource.getInputStream());
            Element namespace = document.getRootElement();

            ElementMapping mapping = new ElementMapping();
            mapping.setMapper(namespace.attributeValue(MAPPER));
            mapping.setSqlList(namespace.elements(SQL));
            return mapping;

        } catch (DocumentException | IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
