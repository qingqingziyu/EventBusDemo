package com.cf.eventbus;

import androidx.annotation.Nullable;

final class Subscription {
    final Object subscriber;
    final SubscriberMethod subscriberMethod;

    volatile boolean active;

    public Subscription(Object subscriber, SubscriberMethod subscriberMethod) {
        this.subscriber = subscriber;
        this.subscriberMethod = subscriberMethod;
        active = true;
    }


    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof Subscription) {
            Subscription subscription = (Subscription) obj;
            return subscriber == subscription.subscriber && subscriberMethod.equals(subscription.subscriberMethod);
        } else {
            return false;
        }
    }


    @Override
    public int hashCode() {
        return subscriber.hashCode() + subscriberMethod.methodString.hashCode();
    }
}
