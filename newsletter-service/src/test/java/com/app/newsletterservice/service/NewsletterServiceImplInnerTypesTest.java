package com.app.newsletterservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class NewsletterServiceImplInnerTypesTest {

    @Test
    void entitlementResponseAccessorsWorkViaReflection() throws Exception {
        Class<?> type = Class.forName("com.app.newsletterservice.service.NewsletterServiceImpl$EntitlementResponse");
        Object instance = instantiate(type);

        invoke(type, instance, "setAdmin", boolean.class, true);
        invoke(type, instance, "setNewsletterEntitled", boolean.class, true);

        assertThat(invoke(type, instance, "isAdmin")).isEqualTo(true);
        assertThat(invoke(type, instance, "isNewsletterEntitled")).isEqualTo(true);
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
