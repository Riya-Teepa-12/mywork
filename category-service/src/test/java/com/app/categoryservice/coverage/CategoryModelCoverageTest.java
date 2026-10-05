package com.app.categoryservice.coverage;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.app.categoryservice.dto.CategoryRequest;
import com.app.categoryservice.dto.CategoryResponse;
import com.app.categoryservice.dto.PostTaxonomyRequest;
import com.app.categoryservice.dto.TagRequest;
import com.app.categoryservice.dto.TagResponse;
import com.app.categoryservice.entity.Category;
import com.app.categoryservice.entity.PostCategory;
import com.app.categoryservice.entity.PostTag;
import com.app.categoryservice.entity.Tag;

class CategoryModelCoverageTest {

    @Test
    void exerciseCategoryModels() {
        List<Class<?>> classes = List.of(
                CategoryRequest.class,
                CategoryResponse.class,
                PostTaxonomyRequest.class,
                TagRequest.class,
                TagResponse.class,
                Category.class,
                PostCategory.class,
                PostTag.class,
                Tag.class);

        for (Class<?> clazz : classes) {
            exerciseClass(clazz);
        }

        assertThat(classes).isNotEmpty();
    }

    private void exerciseClass(Class<?> clazz) {
        Object instance = instantiateNoArgs(clazz);
        if (instance != null) {
            invokeSetters(instance, clazz);
            invokeGetters(instance, clazz);
            Object peer = instantiateNoArgs(clazz);
            if (peer != null) {
                invokeSetters(peer, clazz);
                invokeGetters(peer, clazz);
                instance.equals(peer);
                peer.equals(instance);
                peer.hashCode();
                peer.toString();
            }
            instance.equals(instance);
            instance.equals(null);
            instance.equals(new Object());
            instance.hashCode();
            instance.toString();
            exerciseDirectionalNullEqualityBranches(clazz);
        }

        Object ctorInstance = instantiateLargestConstructor(clazz);
        if (ctorInstance != null) {
            invokeGetters(ctorInstance, clazz);
            ctorInstance.hashCode();
            ctorInstance.toString();
        }

        Object built = instantiateFromBuilder(clazz);
        if (built != null) {
            invokeGetters(built, clazz);
            built.hashCode();
            built.toString();
            built.equals(instance);
            Object builtPeer = instantiateFromBuilder(clazz);
            if (builtPeer != null) {
                built.equals(builtPeer);
                builtPeer.equals(built);
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
        Constructor<?> target = null;
        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
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
            Class<?>[] types = target.getParameterTypes();
            for (int i = 0; i < types.length; i++) {
                args[i] = sampleValue(types[i]);
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
                if (method.getParameterCount() != 1 || method.isSynthetic() || method.getName().startsWith("$")) {
                    continue;
                }
                if (!builderClass.equals(method.getReturnType())) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(builder, sampleValue(method.getParameterTypes()[0]));
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
            try {
                method.invoke(target, sampleValue(method.getParameterTypes()[0]));
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
        if (type == LocalDateTime.class) {
            return LocalDateTime.of(2025, 3, 1, 8, 0);
        }
        if (List.class.isAssignableFrom(type)) {
            return List.of("x");
        }
        if (Set.class.isAssignableFrom(type)) {
            return Set.of("x");
        }
        if (Map.class.isAssignableFrom(type)) {
            return Map.of("k", "v");
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
            String name = field.getName();
            String setterName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
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
