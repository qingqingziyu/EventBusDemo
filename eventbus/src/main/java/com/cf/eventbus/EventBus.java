package com.cf.eventbus;

import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;

import com.cf.eventbus.util.EventBusException;
import com.cf.eventbus.util.SubscriberMethodFinder;

import java.lang.reflect.InvocationHandler;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class EventBus {
    public final String TAG = this.getClass().getSimpleName();


    private static volatile EventBus instance;

    private final Map<Class<?>, CopyOnWriteArrayList<Subscription>> subscriptionByEventType;

    private final Map<Object, List<Class<?>>> typesBySubscriber;

    //粘性
    private final Map<Class<?>, Object> stickyEvents;

    private final SubscriberMethodFinder subscriberMethodFinder;


    //每个线程只有一个队列
    private final ThreadLocal<PostingTheadState> currentPostingThreadState = new ThreadLocal<PostingTheadState>() {
        @Nullable
        @Override
        protected PostingTheadState initialValue() {
            return new PostingTheadState();
        }
    };


    public static EventBus getDefault() {
        if (instance == null) {
            synchronized (EventBus.class) {
                if (instance == null) {
                    instance = new EventBus();
                }
            }
        }
        return instance;
    }


    private EventBus() {
        //初始化一些数据
        subscriptionByEventType = new HashMap<>();
        stickyEvents = new ConcurrentHashMap<>();
        typesBySubscriber = new HashMap<>();
        subscriberMethodFinder = new SubscriberMethodFinder();
    }

    public void register(Object subscriber) {
        //找到所有的方法
        Class<?> subscriberClass = subscriber.getClass();
        List<SubscriberMethod> subscriberMethods = subscriberMethodFinder.findSubscriberMethods(subscriberClass);

        //保存订阅
        synchronized (this) {
            for (SubscriberMethod subscriberMethod : subscriberMethods) {
                subscriber(subscriber, subscriberMethod);
            }
        }

    }

    /**
     * @作者：陈飞
     * @说明：保存订阅方法
     * @创建日期: 2020/1/6 14:36
     */
    private void subscriber(Object subscriber, SubscriberMethod subscriberMethod) {
        //1、保存数据 ,如果重复，抛出异常
        Class<?> eventType = subscriberMethod.eventType;
        Subscription newSubScription = new Subscription(subscriber, subscriberMethod);
        CopyOnWriteArrayList<Subscription> subscriptions = subscriptionByEventType.get(eventType);
        if (subscriptions == null) {
            subscriptions = new CopyOnWriteArrayList<>();
            subscriptionByEventType.put(eventType, subscriptions);
        } else {
            if (subscriptions.contains(newSubScription)) {
                throw new EventBusException("Subscriber" + subscriber.getClass() + " already registered to event " + eventType);
            }
        }

        int size = subscriptions.size();
        for (int i = 0; i < size; i++) {
            if (i == size || subscriberMethod.priority > subscriptions.get(i).subscriberMethod.priority) {
                subscriptions.add(i, newSubScription);
                break;
            }
        }

        List<Class<?>> subscribedEvents = typesBySubscriber.get(subscriber);
        if (subscribedEvents == null) {
            subscribedEvents = new ArrayList<>();
            typesBySubscriber.put(subscriber, subscribedEvents);
        }
        subscribedEvents.add(eventType);

        //执行粘性事件
        if (subscriberMethod.sticky) {
            Set<Map.Entry<Class<?>, Object>> entries = stickyEvents.entrySet();
            for (Map.Entry<Class<?>, Object> entry : entries) {
                Class<?> candidateEventType = entry.getKey();
                if (eventType.isAssignableFrom(candidateEventType)) {
                    Object stickyEvent = entry.getValue();

                }
            }
        }
    }


    private void checkPostStickyEventToSubscription(Subscription subscription, Object stickyEvent) {
        if (stickyEvent != null) {
            postToSubscription(subscription, stickyEvent, Looper.getMainLooper() == Looper.myLooper());
        }
    }

    private void postToSubscription(Subscription newSubscription, Object stickyEvent, boolean b) {
        InvokeHelper.getDefault().post(newSubscription, stickyEvent, b);
    }


    public synchronized void unRegister(Object subscriber) {
        //去除订阅
        List<Class<?>> subscribedType = typesBySubscriber.get(subscriber);
        if (subscribedType != null) {
            for (Class<?> eventType : subscribedType) {
                unsubscribeByEventType(subscriber, eventType);
            }
            typesBySubscriber.remove(subscriber);
        } else {
            Log.w(TAG, "Subscriber to ungister was not registered before:" + subscriber.getClass());
        }

    }

    private void unsubscribeByEventType(Object subscriber, Class<?> eventType) {
        List<Subscription> subscriptions = subscriptionByEventType.get(eventType);
        if (subscriptions != null) {
            int size = subscriptions.size();
            for (int i = 0; i < size; i++) {
                Subscription subscription = subscriptions.get(i);
                if (subscription.subscriber == subscriber) {
                    subscription.active = false;
                    subscriptions.remove(i);
                    i--;
                    size--;
                }
            }
        }
    }

    public void post(Object object) {
        //1.放入执行队列
        PostingTheadState postingTheadState = currentPostingThreadState.get();
        List<Object> eventQueue = postingTheadState.eventQueue;
        eventQueue.add(eventQueue);

        if (!postingTheadState.isPosting) {
            postingTheadState.isMainThread = Looper.getMainLooper() == Looper.myLooper();
            postingTheadState.isPosting = true;
            if (postingTheadState.cancled) {
                throw new EventBusException("Internal error.Abort state was not reset");
            }
            try {
                while (!eventQueue.isEmpty()) {
                    postSingleEvent(eventQueue.remove(0), postingTheadState);
                }
            } finally {
                postingTheadState.isPosting = false;
                postingTheadState.isMainThread = false;
            }
        }
    }

    private void postSingleEvent(Object event, PostingTheadState postingTheadState) throws Error {
        Class<?> eventClass = event.getClass();
        boolean subscriptionFound = false;
        subscriptionFound |= postSingleEventForEventType(event, postingTheadState, eventClass);
        if (!subscriptionFound) {

        }
    }

    private boolean postSingleEventForEventType(Object event, PostingTheadState postingTheadState, Class<?> eventClass) {
        CopyOnWriteArrayList<Subscription> subscriptions;
        synchronized (this) {
            subscriptions = subscriptionByEventType.get(eventClass);
        }
        if (subscriptions != null && !subscriptions.isEmpty()) {
            for (Subscription subscription : subscriptions) {
                postingTheadState.event = event;
                postingTheadState.subscription = subscription;
                boolean aborted = false;
                try {
                    postToSubscription(subscription, event, postingTheadState.isMainThread);
                    aborted = postingTheadState.cancled;
                } finally {
                    postingTheadState.event = null;
                    postingTheadState.subscription = null;
                    postingTheadState.cancled = false;
                }
                if (aborted) {
                    break;
                }
            }
            return true;
        }
        return false;
    }

    public void postSticky(Object event) {
        //加入粘性缓存 stickyEvent
        synchronized (stickyEvents) {
            stickyEvents.put(event.getClass(), event);
        }
        //执行
        post(event);
    }

    public void cancelEventDelivery(Object event) {
        PostingTheadState postingTheadState = currentPostingThreadState.get();
        if (!postingTheadState.isPosting) {
            throw new EventBusException("This method may only be called " +
                    "from inside event handing methods on the posting thread");
        } else if (event == null) {
            throw new EventBusException("Event may not be null");
        } else if (postingTheadState.event != event) {
            throw new EventBusException("Only the currently handled event my be aborted");
        }
    }

    final static class PostingTheadState {
        final List<Object> eventQueue = new ArrayList<>();
        boolean isPosting;
        boolean isMainThread;
        Subscription subscription;
        Object event;
        boolean cancled;
    }
}
