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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;
import rx.util.functions.Action0;
import rx.util.functions.Func0;
import rx.util.functions.Func1;

public class OperatorTimeoutWithSelectorTest {
    @Test(timeout = 2000)
    public void testTimeoutSelectorNormal1() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);
        source.onNext(2);
        source.onNext(3);
        timeout.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o).onNext(3);
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorTimeoutFirst() throws InterruptedException {
        Observable<Integer> source = Observable.<Integer>never();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);
        
        timeout.onNext(1);
        
        inOrder.verify(o).onNext(100);
        inOrder.verify(o).onCompleted();
        verify(o, never()).onError(any(Throwable.class));

    }

    @Test
    public void testTimeoutSelectorFirstThrows() {
        Observable<Integer> source = Observable.<Integer>never();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                throw new OperationReduceTest.CustomException();
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorSubsequentThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                throw new OperationReduceTest.CustomException();
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorFirstObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return Observable.<Integer> error(new OperationReduceTest.CustomException());
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onNext(any());
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorSubsequentObservableThrows() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return Observable.<Integer> error(new OperationReduceTest.CustomException());
            }
        };

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Observable<Integer> other = Observable.from(Arrays.asList(100));

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        InOrder inOrder = inOrder(o);

        source.timeout(firstTimeoutFunc, timeoutFunc, other).subscribe(o);

        source.onNext(1);

        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(any(OperationReduceTest.CustomException.class));
        verify(o, never()).onCompleted();

    }

    @Test
    public void testTimeoutSelectorWithFirstTimeoutFirstAndNoOtherObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return timeout;
            }
        };

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return PublishSubject.create();
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        source.timeout(firstTimeoutFunc, timeoutFunc).subscribe(o);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutFirstAndNoOtherObservable() {
        PublishSubject<Integer> source = PublishSubject.create();
        final PublishSubject<Integer> timeout = PublishSubject.create();

        Func0<Observable<Integer>> firstTimeoutFunc = new Func0<Observable<Integer>>() {
            @Override
            public Observable<Integer> call() {
                return PublishSubject.create();
            }
        };

        Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                return timeout;
            }
        };

        @SuppressWarnings("unchecked")
        Observer<Object> o = mock(Observer.class);
        source.timeout(firstTimeoutFunc, timeoutFunc).subscribe(o);
        source.onNext(1);

        timeout.onNext(1);

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onError(isA(TimeoutException.class));
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void testTimeoutSelectorWithTimeoutAndOnNextRaceCondition() throws InterruptedException {
        // Thread 1                                    Thread 2
        //
        // observer.onNext(1)
        // start timeout
        // unsubscribe timeout in thread 2          start to do some long-time work in "unsubscribe"
        // observer.onNext(2)
        // timeout.onNext(1)
        //                                          "unsubscribe" done
        //
        //
        // In the above case, the timeout operator should ignore "timeout.onNext(1)"
        // since "observer" has already seen 2.
        final CountDownLatch observerReceivedTwo = new CountDownLatch(1);
        final CountDownLatch timeoutEmittedOne = new CountDownLatch(1);
        final CountDownLatch observerCompleted = new CountDownLatch(1);

        final Func1<Integer, Observable<Integer>> timeoutFunc = new Func1<Integer, Observable<Integer>>() {
            @Override
            public Observable<Integer> call(Integer t1) {
                if (t1 == 1) {
                    // Force "unsubscribe" run on another thread
                    return Observable.create(new OnSubscribe<Integer>(){
                        @Override
                        public void call(Subscriber<? super Integer> subscriber) {
                            subscriber.add(Subscriptions.create(new Action0(){
                                @Override
                                public void call() {
                                    try {
                                        // emulate "unsubscribe" is busy and finishes after timeout.onNext(1)
                                        timeoutEmittedOne.await();
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }}));
                            // force the timeout message be sent after observer.onNext(2)
                            try {
                                observerReceivedTwo.await();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            if(!subscriber.isUnsubscribed()) {
                                subscriber.onNext(1);
                                timeoutEmittedOne.countDown();
                            }
                        }
                    }).subscribeOn(Schedulers.newThread());
                } else {
                    return PublishSubject.create();
                }
            }
        };

        @SuppressWarnings("unchecked")
        final Observer<Integer> o = mock(Observer.class);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                observerReceivedTwo.countDown();
                return null;
            }

        }).when(o).onNext(2);
        doAnswer(new Answer<Void>() {

            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                observerCompleted.countDown();
                return null;
            }

        }).when(o).onCompleted();

        new Thread(new Runnable() {

            @Override
            public void run() {
                PublishSubject<Integer> source = PublishSubject.create();
                source.timeout(timeoutFunc, Observable.from(3)).subscribe(o);
                source.onNext(1); // start timeout
                source.onNext(2); // disable timeout
                try {
                    timeoutEmittedOne.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                source.onCompleted();
            }

        }).start();

        observerCompleted.await();

        InOrder inOrder = inOrder(o);
        inOrder.verify(o).onNext(1);
        inOrder.verify(o).onNext(2);
        inOrder.verify(o, never()).onNext(3);
        inOrder.verify(o).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }
}
