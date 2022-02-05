package com.springboot.mjt.loader;

import com.springboot.mjt.exception.ElementResolvedException;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.loader
 * @Author xuzhenkui
 * @Date 2021/9/16 10:16
 */
public class ResourceLoader {

    private static final String NAMESPACE = "namespace";

    private static final String MAPPER = "mapper";

    private static final String SQL = "sql";

    public static ElementMapping load(Resource resource) {
        SAXReader reader = new SAXReader();
        try {
            Document document = reader.read(resource.getInputStream());
            Element namespace = document.getRootElement();

            if (!Objects.equals(NAMESPACE, namespace.getQName().getName())) {
                throw new ElementResolvedException(NAMESPACE + " element name defines not correct");
            }

            if (!StringUtils.hasText(namespace.attributeValue(MAPPER))) {
                throw new ElementResolvedException(MAPPER + " attribute value must not be null");
            }

            return new ElementMapping(namespace.attributeValue(MAPPER), namespace.elements(SQL));
        } catch (DocumentException | IOException | ElementResolvedException e) {
            e.printStackTrace();
        }
        return null;
    }
}
