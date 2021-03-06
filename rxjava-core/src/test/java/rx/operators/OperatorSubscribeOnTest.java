/**
 * Copyright 2014 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package rx.operators;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.Subscription;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;

public class OperatorSubscribeOnTest {

    private class ThreadSubscription implements Subscription {
        private volatile Thread thread;

        private final CountDownLatch latch = new CountDownLatch(1);

        private final Subscription s = Subscriptions.create(new Action0() {

            @Override
            public void call() {
                thread = Thread.currentThread();
                latch.countDown();
            }

        });

        @Override
        public void unsubscribe() {
            s.unsubscribe();
        }

        @Override
        public boolean isUnsubscribed() {
            return s.isUnsubscribed();
        }

        public Thread getThread() throws InterruptedException {
            latch.await();
            return thread;
        }
    }

    @Test
    public void testSubscribeOnAndVerifySubscribeAndUnsubscribeThreads()
            throws InterruptedException {
        final ThreadSubscription subscription = new ThreadSubscription();
        final AtomicReference<Thread> subscribeThread = new AtomicReference<Thread>();
        Observable<Integer> w = Observable.create(new OnSubscribe<Integer>() {

            @Override
            public void call(Subscriber<? super Integer> t1) {
                subscribeThread.set(Thread.currentThread());
                t1.add(subscription);
                t1.onNext(1);
                t1.onNext(2);
                t1.onCompleted();
            }
        });

        TestObserver<Integer> observer = new TestObserver<Integer>();
        w.subscribeOn(Schedulers.newThread()).subscribe(observer);

        Thread unsubscribeThread = subscription.getThread();

        assertNotNull(unsubscribeThread);
        assertNotSame(Thread.currentThread(), unsubscribeThread);

        assertNotNull(subscribeThread.get());
        assertNotSame(Thread.currentThread(), subscribeThread.get());
        // True for Schedulers.newThread()
        assertTrue(unsubscribeThread == subscribeThread.get());

        observer.assertReceivedOnNext(Arrays.asList(1, 2));
        observer.assertTerminalEvent();
    }

    @Test
    public void testIssue813() throws InterruptedException {
        // https://github.com/Netflix/RxJava/issues/813
        final CountDownLatch latch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(1);

        TestObserver<Integer> observer = new TestObserver<Integer>();
        final ThreadSubscription s = new ThreadSubscription();

        final Subscription subscription = Observable
                .create(new Observable.OnSubscribe<Integer>() {
                    @Override
                    public void call(
                            final Subscriber<? super Integer> subscriber) {
                        subscriber.add(s);
                        try {
                            latch.await();
                            // Already called "unsubscribe", "isUnsubscribed"
                            // shouble be true
                            if (!subscriber.isUnsubscribed()) {
                                throw new IllegalStateException(
                                        "subscriber.isUnsubscribed should be true");
                            }
                            subscriber.onCompleted();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } catch (Throwable e) {
                            subscriber.onError(e);
                        } finally {
                            doneLatch.countDown();
                        }
                    }
                }).subscribeOn(Schedulers.computation()).subscribe(observer);

        subscription.unsubscribe();
        // As unsubscribe is called in other thread, we need to wait for it.
        s.getThread();
        latch.countDown();
        doneLatch.await();
        assertEquals(0, observer.getOnErrorEvents().size());
        assertEquals(1, observer.getOnCompletedEvents().size());
    }
}
