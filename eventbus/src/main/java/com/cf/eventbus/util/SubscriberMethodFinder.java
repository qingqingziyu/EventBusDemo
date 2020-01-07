package com.cf.eventbus.util;

import com.cf.eventbus.EventBus;
import com.cf.eventbus.Subscribe;
import com.cf.eventbus.SubscriberMethod;
import com.cf.eventbus.ThreadMode;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @作者：陈飞
 * @说明：订阅方法查找类
 * @创建日期: 2020/1/6 13:20
 */
public class SubscriberMethodFinder {

    private static final int MODIFIERS_IGONE = Modifier.ABSTRACT | Modifier.STATIC;

    private static final Map<Class<?>, List<SubscriberMethod>> METHOD_CACHE = new ConcurrentHashMap<>();

    public List<SubscriberMethod> findSubscriberMethods(Class<?> subscriberClass) {
        List<SubscriberMethod> subscriberMethods = METHOD_CACHE.get(subscriberClass);
        //查找成功
        if (subscriberMethods != null) {
            return subscriberMethods;
        }

        subscriberMethods = findUsingReflection(subscriberClass);
        if (subscriberMethods.isEmpty()) {
            throw new EventBusException("Subscriber " + subscriberClass + " and its super classes have no public methods with the @Subscribe annotation");
        } else {
            METHOD_CACHE.put(subscriberClass, subscriberMethods);
            return subscriberMethods;
        }

    }


    private List<SubscriberMethod> findUsingReflection(Class<?> subcriberClass) {
        List<SubscriberMethod> subscriberMethods = new ArrayList<>();
        Class<?> clazz = subcriberClass;
        //是否跳过
        boolean skipSuperClasses = false;

        while (clazz != null) {
            Method[] methods;
            try {
                methods = subcriberClass.getDeclaredMethods();
            } catch (Throwable th) {
                methods = subcriberClass.getMethods();
                skipSuperClasses = true;
            }

            for (Method method : methods) {
                int modifiers = method.getModifiers();
                if ((modifiers & Modifier.PUBLIC) != 0 && (modifiers & MODIFIERS_IGONE) == 0) {
                    Class<?>[] parameterTypes = method.getParameterTypes();
                    if (parameterTypes.length == 1) {
                        Subscribe subscribeAnnotation = method.getAnnotation(Subscribe.class);
                        if (subscribeAnnotation != null) {
                            Class<?> eventType = parameterTypes[0];
                            ThreadMode threadMode = subscribeAnnotation.threadMode();
                            subscriberMethods.add(new SubscriberMethod(method, eventType, threadMode, subscribeAnnotation.priority(), subscribeAnnotation.sticky()));
                        }
                    } else if (method.isAnnotationPresent(Subscribe.class)) {
                        String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                        throw new EventBusException("@Subcriber method" + methodName
                                + "must have exactly 1 parameter but has " + parameterTypes.length);
                    }
                } else if (method.isAnnotationPresent(Subscribe.class)) {
                    String methodName = method.getDeclaringClass().getName() + "." + method.getName();
                    throw new EventBusException(methodName + "is a illegal @Subscribe method: must be public,no-static,and non-abstract");
                }
            }

            if (skipSuperClasses) {
                clazz = null;
            } else {
                clazz = clazz.getSuperclass();
                String clazzName = clazz.getName();
                if (clazzName.startsWith("java.") || clazzName.startsWith("javax.") || clazzName.startsWith("android.")) {
                    clazz = null;
                }
            }
        }
        return subscriberMethods;
    }
}
