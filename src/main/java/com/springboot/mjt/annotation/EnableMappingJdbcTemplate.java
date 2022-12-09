package com.springboot.mjt.annotation;

import com.springboot.mjt.selector.EnableMappingJdbcTemplateSelector;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.annotation
 * @Author xuzhenkui
 * @Date 2021/9/13 15:22
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(EnableMappingJdbcTemplateSelector.class)
public @interface EnableMappingJdbcTemplate {
    String[] baseLocations() default {};
    boolean exchange() default true;
    boolean format() default true;
}
