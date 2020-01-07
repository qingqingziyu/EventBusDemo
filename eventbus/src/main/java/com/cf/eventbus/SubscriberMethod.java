package com.cf.eventbus;

import androidx.annotation.Nullable;

import java.lang.reflect.Method;

public class SubscriberMethod {
    public final Method method;
    public final ThreadMode threadMode;
    public final Class<?> eventType;//参数
    public final int priority;
    public final boolean sticky;

    public String methodString;


    public SubscriberMethod(Method method,Class<?> eventType, ThreadMode threadMode,  int priority, boolean sticky) {
        this.method = method;
        this.threadMode = threadMode;
        this.eventType = eventType;
        this.priority = priority;
        this.sticky = sticky;
    }


    @Override
    public boolean equals(@Nullable Object other) {
        if (other == this) {
            return true;
        } else if (other instanceof SubscriberMethod) {
            checkMethodString();
            SubscriberMethod otherSubscriberMethod = (SubscriberMethod) other;
            otherSubscriberMethod.checkMethodString();
            return methodString.equals(otherSubscriberMethod.methodString);
        } else {
            return false;
        }
    }

    private synchronized void checkMethodString() {
        if (methodString == null) {
            StringBuilder builder = new StringBuilder(64);
            builder.append(method.getDeclaringClass().getName());
            builder.append('#').append(method.getName());
            builder.append('(').append(eventType.getName());
            methodString = builder.toString();
        }
    }

    @Override
    public int hashCode() {
        return method.hashCode();
    }
}
