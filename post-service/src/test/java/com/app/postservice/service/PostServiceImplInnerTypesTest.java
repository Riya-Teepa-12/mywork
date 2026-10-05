package com.app.postservice.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

class PostServiceImplInnerTypesTest {

    @Test
    void sendPostNotificationRequestAccessorsWorkViaReflection() throws Exception {
        Class<?> type = Class.forName("com.app.postservice.service.PostServiceImpl$SendPostNotificationRequest");
        Object instance = instantiate(type);

        invoke(type, instance, "setPostId", Long.class, 42L);
        invoke(type, instance, "setTitle", String.class, "Title");
        invoke(type, instance, "setSlug", String.class, "slug");
        invoke(type, instance, "setExcerpt", String.class, "excerpt");

        assertThat(invoke(type, instance, "getPostId")).isEqualTo(42L);
        assertThat(invoke(type, instance, "getTitle")).isEqualTo("Title");
        assertThat(invoke(type, instance, "getSlug")).isEqualTo("slug");
        assertThat(invoke(type, instance, "getExcerpt")).isEqualTo("excerpt");
    }

    @Test
    void entitlementResponseAccessorsWorkViaReflection() throws Exception {
        Class<?> type = Class.forName("com.app.postservice.service.PostServiceImpl$EntitlementResponse");
        Object instance = instantiate(type);

        invoke(type, instance, "setAdmin", boolean.class, true);
        invoke(type, instance, "setAuthorPostSubscriptionActive", boolean.class, true);

        assertThat(invoke(type, instance, "isAdmin")).isEqualTo(true);
        assertThat(invoke(type, instance, "isAuthorPostSubscriptionActive")).isEqualTo(true);
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
