package com.springboot.mjt.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description Regular Expression match
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.annotation
 * @Author Xu Zhenkui
 * @Date 2023-01-06 10:09
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface MjtMapperScan {
    String[] value() default {};
}
