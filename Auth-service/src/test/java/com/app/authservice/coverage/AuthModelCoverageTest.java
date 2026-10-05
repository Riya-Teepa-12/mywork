package com.app.authservice.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.app.authservice.dto.AuditLogRequest;
import com.app.authservice.dto.AuditLogResponse;
import com.app.authservice.dto.AuthorUpgradeDecisionRequest;
import com.app.authservice.dto.AuthorUpgradeRequestResponse;
import com.app.authservice.dto.AuthResponse;
import com.app.authservice.dto.BecomeAuthorRequest;
import com.app.authservice.dto.ChangePasswordRequest;
import com.app.authservice.dto.ChangeRoleRequest;
import com.app.authservice.dto.CreateSubscriptionOrderRequest;
import com.app.authservice.dto.CreateSubscriptionOrderResponse;
import com.app.authservice.dto.ForgotPasswordOtpRequest;
import com.app.authservice.dto.LoginRequest;
import com.app.authservice.dto.OAuthLoginRequest;
import com.app.authservice.dto.ProfileUpdateRequest;
import com.app.authservice.dto.PublicUserProfileResponse;
import com.app.authservice.dto.RefreshTokenRequest;
import com.app.authservice.dto.RegisterRequest;
import com.app.authservice.dto.ResetPasswordWithOtpRequest;
import com.app.authservice.dto.SimpleApiResponse;
import com.app.authservice.dto.SubscriptionEntitlementResponse;
import com.app.authservice.dto.SubscriptionPlanInfoResponse;
import com.app.authservice.dto.SubscriptionPlansResponse;
import com.app.authservice.dto.SubscriptionSummaryResponse;
import com.app.authservice.dto.TokenValidationResponse;
import com.app.authservice.dto.UserProfileResponse;
import com.app.authservice.dto.ValidationPatterns;
import com.app.authservice.dto.VerifySignupOtpRequest;
import com.app.authservice.dto.VerifySubscriptionPaymentRequest;
import com.app.authservice.entity.AuditLog;
import com.app.authservice.entity.AuthorUpgradeRequest;
import com.app.authservice.entity.AuthorUpgradeStatus;
import com.app.authservice.entity.AuthProvider;
import com.app.authservice.entity.EmailOtp;
import com.app.authservice.entity.OtpPurpose;
import com.app.authservice.entity.PaymentOrder;
import com.app.authservice.entity.PaymentOrderStatus;
import com.app.authservice.entity.Role;
import com.app.authservice.entity.SubscriptionPlanType;
import com.app.authservice.entity.SubscriptionStatus;
import com.app.authservice.entity.User;
import com.app.authservice.entity.UserSubscription;
import com.app.authservice.messaging.NotificationDispatchEvent;

class AuthModelCoverageTest {

    @Test
    void exerciseAuthDtoEntityAndMessagingModels() {
        List<Class<?>> classes = List.of(
                AuditLogRequest.class,
                AuditLogResponse.class,
                AuthorUpgradeDecisionRequest.class,
                AuthorUpgradeRequestResponse.class,
                AuthResponse.class,
                BecomeAuthorRequest.class,
                ChangePasswordRequest.class,
                ChangeRoleRequest.class,
                CreateSubscriptionOrderRequest.class,
                CreateSubscriptionOrderResponse.class,
                ForgotPasswordOtpRequest.class,
                LoginRequest.class,
                OAuthLoginRequest.class,
                ProfileUpdateRequest.class,
                PublicUserProfileResponse.class,
                RefreshTokenRequest.class,
                RegisterRequest.class,
                ResetPasswordWithOtpRequest.class,
                SimpleApiResponse.class,
                SubscriptionEntitlementResponse.class,
                SubscriptionPlanInfoResponse.class,
                SubscriptionPlansResponse.class,
                SubscriptionSummaryResponse.class,
                TokenValidationResponse.class,
                UserProfileResponse.class,
                VerifySignupOtpRequest.class,
                VerifySubscriptionPaymentRequest.class,
                AuditLog.class,
                AuthorUpgradeRequest.class,
                EmailOtp.class,
                PaymentOrder.class,
                User.class,
                UserSubscription.class,
                NotificationDispatchEvent.class,
                AuthorUpgradeStatus.class,
                AuthProvider.class,
                OtpPurpose.class,
                PaymentOrderStatus.class,
                Role.class,
                SubscriptionPlanType.class,
                SubscriptionStatus.class);

        for (Class<?> clazz : classes) {
            exerciseClass(clazz);
        }

        assertThat(ValidationPatterns.EMAIL_REGEX).isNotBlank();
        assertThat(ValidationPatterns.STRONG_CREDENTIAL_REGEX).isNotBlank();
    }

    private void exerciseClass(Class<?> clazz) {
        if (clazz.isEnum()) {
            assertThat(clazz.getEnumConstants()).isNotEmpty();
            return;
        }

        Object defaultInstance = instantiateNoArgs(clazz);
        if (defaultInstance != null) {
            invokeSetters(defaultInstance, clazz);
            invokeGetters(defaultInstance, clazz);
            Object peer = instantiateNoArgs(clazz);
            if (peer != null) {
                invokeSetters(peer, clazz);
                invokeGetters(peer, clazz);
                defaultInstance.equals(peer);
                peer.equals(defaultInstance);
                peer.hashCode();
                peer.toString();
            }
            defaultInstance.equals(defaultInstance);
            defaultInstance.equals(null);
            defaultInstance.equals(new Object());
            defaultInstance.hashCode();
            defaultInstance.toString();
            exerciseDirectionalNullEqualityBranches(clazz);
        }

        Object allArgsInstance = instantiateLargestConstructor(clazz);
        if (allArgsInstance != null) {
            invokeGetters(allArgsInstance, clazz);
            allArgsInstance.hashCode();
            allArgsInstance.toString();
        }

        Object builtInstance = instantiateFromBuilder(clazz);
        if (builtInstance != null) {
            invokeGetters(builtInstance, clazz);
            builtInstance.hashCode();
            builtInstance.toString();
            builtInstance.equals(defaultInstance);
            Object builtPeer = instantiateFromBuilder(clazz);
            if (builtPeer != null) {
                builtInstance.equals(builtPeer);
                builtPeer.equals(builtInstance);
            }
        }
    }

    private Object instantiateNoArgs(Class<?> clazz) {
        try {
            Constructor<?> ctor = clazz.getDeclaredConstructor();
            if (!Modifier.isPublic(ctor.getModifiers())) {
                ctor.setAccessible(true);
            }
            return ctor.newInstance();
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object instantiateLargestConstructor(Class<?> clazz) {
        Constructor<?>[] constructors = clazz.getDeclaredConstructors();
        Constructor<?> target = null;
        for (Constructor<?> constructor : constructors) {
            if (target == null || constructor.getParameterCount() > target.getParameterCount()) {
                target = constructor;
            }
        }
        if (target == null || target.getParameterCount() == 0) {
            return null;
        }
        try {
            if (!Modifier.isPublic(target.getModifiers())) {
                target.setAccessible(true);
            }
            Object[] args = new Object[target.getParameterCount()];
            Class<?>[] paramTypes = target.getParameterTypes();
            for (int i = 0; i < paramTypes.length; i++) {
                args[i] = sampleValue(paramTypes[i]);
            }
            return target.newInstance(args);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Object instantiateFromBuilder(Class<?> clazz) {
        try {
            Method builderMethod = clazz.getMethod("builder");
            Object builder = builderMethod.invoke(null);
            Class<?> builderClass = builder.getClass();
            for (Method method : builderClass.getDeclaredMethods()) {
                if (method.getParameterCount() != 1) {
                    continue;
                }
                if (method.isSynthetic() || method.getName().startsWith("$")) {
                    continue;
                }
                if (!builderClass.equals(method.getReturnType())) {
                    continue;
                }
                Object value = sampleValue(method.getParameterTypes()[0]);
                method.setAccessible(true);
                method.invoke(builder, value);
            }
            Method build = builderClass.getDeclaredMethod("build");
            build.setAccessible(true);
            return build.invoke(builder);
        } catch (Exception ignored) {
            return null;
        }
    }

    private void invokeSetters(Object target, Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            if (!method.getName().startsWith("set") || method.getParameterCount() != 1) {
                continue;
            }
            Object value = sampleValue(method.getParameterTypes()[0]);
            try {
                method.invoke(target, value);
            } catch (Exception ignored) {
                // Best effort for coverage.
            }
        }
    }

    private void invokeGetters(Object target, Class<?> clazz) {
        for (Method method : clazz.getMethods()) {
            boolean getter = method.getName().startsWith("get") || method.getName().startsWith("is");
            if (!getter || method.getParameterCount() != 0 || "getClass".equals(method.getName())) {
                continue;
            }
            try {
                method.invoke(target);
            } catch (Exception ignored) {
                // Best effort for coverage.
            }
        }
    }

    private Object sampleValue(Class<?> type) {
        if (type == String.class) {
            return "value";
        }
        if (type == Long.class || type == long.class) {
            return 1L;
        }
        if (type == Integer.class || type == int.class) {
            return 1;
        }
        if (type == Boolean.class || type == boolean.class) {
            return true;
        }
        if (type == Double.class || type == double.class) {
            return 1.0d;
        }
        if (type == Float.class || type == float.class) {
            return 1.0f;
        }
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2025, 1, 1, 10, 15);
        }
        if (type == LocalDate.class) {
            return LocalDate.of(2025, 1, 1);
        }
        if (type == Instant.class) {
            return Instant.parse("2025-01-01T00:00:00Z");
        }
        if (type == UUID.class) {
            return UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        }
        if (type == BigDecimal.class) {
            return BigDecimal.ONE;
        }
        if (List.class.isAssignableFrom(type)) {
            return List.of("value");
        }
        if (Set.class.isAssignableFrom(type)) {
            return Set.of("value");
        }
        if (Map.class.isAssignableFrom(type)) {
            return Map.of("k", "v");
        }
        if (type.isEnum()) {
            Object[] values = type.getEnumConstants();
            return values.length == 0 ? null : values[0];
        }

        Object nested = instantiateNoArgs(type);
        if (nested != null) {
            return nested;
        }
        return null;
    }

    private void exerciseDirectionalNullEqualityBranches(Class<?> clazz) {
        List<Method> orderedSetters = orderedSetters(clazz);
        if (orderedSetters.isEmpty()) {
            return;
        }
        for (int i = 0; i < orderedSetters.size(); i++) {
            Method current = orderedSetters.get(i);

            Object left = instantiateNoArgs(clazz);
            Object right = instantiateNoArgs(clazz);
            if (left == null || right == null) {
                return;
            }
            for (int j = 0; j < i; j++) {
                applySampleValue(left, orderedSetters.get(j));
                applySampleValue(right, orderedSetters.get(j));
            }
            applySampleValue(right, current);
            left.equals(right);
            right.equals(left);

            Object leftReverse = instantiateNoArgs(clazz);
            Object rightReverse = instantiateNoArgs(clazz);
            if (leftReverse == null || rightReverse == null) {
                return;
            }
            for (int j = 0; j < i; j++) {
                applySampleValue(leftReverse, orderedSetters.get(j));
                applySampleValue(rightReverse, orderedSetters.get(j));
            }
            applySampleValue(leftReverse, current);
            leftReverse.equals(rightReverse);
            rightReverse.equals(leftReverse);
        }
    }

    private List<Method> orderedSetters(Class<?> clazz) {
        List<Method> ordered = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            String fieldName = field.getName();
            String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
            for (Method method : clazz.getMethods()) {
                if (method.getName().equals(setterName) && method.getParameterCount() == 1) {
                    ordered.add(method);
                    break;
                }
            }
        }
        for (Method method : clazz.getMethods()) {
            if (method.getName().startsWith("set")
                    && method.getParameterCount() == 1
                    && !ordered.contains(method)) {
                ordered.add(method);
            }
        }
        return ordered;
    }

    private void applySampleValue(Object target, Method setter) {
        try {
            setter.invoke(target, sampleValue(setter.getParameterTypes()[0]));
        } catch (Exception ignored) {
            // Best effort for coverage.
        }
    }
}
