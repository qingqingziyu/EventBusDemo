package com.cf.eventbus;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 方法执行帮助类
 */
final class InvokeHelper {
    private final static ExecutorService DEFAULT_EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    private static volatile InvokeHelper instance;

    private HandlerPoster handlerPoster;

    public static InvokeHelper getDefault() {
        if (instance == null) {
            synchronized (InvokeHelper.class) {
                if (instance == null) {
                    instance = new InvokeHelper();
                }
            }
        }
        return instance;
    }

    private InvokeHelper() {
        handlerPoster = new HandlerPoster(Looper.getMainLooper());
    }


    public void post(final Subscription subscription, final Object event, boolean isMainThread) {
        switch (subscription.subscriberMethod.threadMode) {
            case POSITION:
                //直接执行
                invokeSubcriber(subscription, event);
                break;
            case MAIN:
                if (isMainThread) {
                    //直接执行
                    invokeSubcriber(subscription, event);
                } else {
                    //放在handler执行
                    handlerPoster.post(new Runnable() {
                        @Override
                        public void run() {
                            invokeSubcriber(subscription, event);
                        }
                    });
                }
                break;
            case BACKGROUND:
                if (isMainThread) {
                    //放在后台线程执行

                } else {
                    //执行
                    invokeSubcriber(subscription, event);
                }
                break;
            case ASYNC:
                //放在异步线程执行
                getExecutorService().execute(new Runnable() {
                    @Override
                    public void run() {
                        invokeSubcriber(subscription, event);
                    }
                });
                break;
            default:
                //抛异常
                throw new IllegalStateException("Unknown thread mode:" + subscription.subscriberMethod.threadMode);
        }
    }

    private void invokeSubcriber(Subscription subscription, Object event) {
        try {
            subscription.subscriberMethod.method.invoke(subscription.subscriber, event);
        } catch (InvocationTargetException e) {

        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    ExecutorService getExecutorService() {
        return DEFAULT_EXECUTOR_SERVICE;
    }

    class HandlerPoster extends Handler {
        public HandlerPoster(@NonNull Looper looper) {
            super(looper);
        }
    }
}
