package com.springboot.mjt.exception;

/**
 * @Description
 * @Project mjt-spring-boot-starter
 * @Package com.springboot.mjt.exception
 * @Author xuzhenkui
 * @Date 2022/2/5 21:38
 */
public class ElementResolvedException extends Exception {

    /**
     * Constructs a new exception with {@code null} as its detail message.
     * The cause is not initialized, and may subsequently be initialized by a
     * call to {@link #initCause}.
     */
    public ElementResolvedException(String message) {
        super(message);
    }
}
