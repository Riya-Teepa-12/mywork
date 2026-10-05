package com.app.commentservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class CommentServiceImplInnerTypesTest {

    @Test
    void postSummaryResponseAccessorsWorkViaReflection() throws Exception {
        Class<?> type = Class.forName("com.app.commentservice.service.CommentServiceImpl$PostSummaryResponse");
        Object instance = instantiate(type);

        invoke(type, instance, "setAuthorId", Long.class, 7L);
        invoke(type, instance, "setTitle", String.class, "My Post");

        assertThat(invoke(type, instance, "getAuthorId")).isEqualTo(7L);
        assertThat(invoke(type, instance, "getTitle")).isEqualTo("My Post");
    }

    @Test
    void postAuthorResponseAccessorsWorkViaReflection() throws Exception {
        Class<?> type = Class.forName("com.app.commentservice.service.CommentServiceImpl$PostAuthorResponse");
        Object instance = instantiate(type);

        invoke(type, instance, "setAuthorId", Long.class, 9L);

        assertThat(invoke(type, instance, "getAuthorId")).isEqualTo(9L);
    }

    private Object instantiate(Class<?> type) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructor();
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    private Object invoke(Class<?> type, Object target, String methodName, Class<?> parameterType, Object argument)
            throws Exception {
        Method method = type.getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return method.invoke(target, argument);
    }

    private Object invoke(Class<?> type, Object target, String methodName) throws Exception {
        Method method = type.getDeclaredMethod(methodName);
        method.setAccessible(true);
        return method.invoke(target);
    }
}
