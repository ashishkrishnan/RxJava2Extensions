/*
 * Copyright 2016 David Karnok
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package hu.akarnokd.rxjava2.operators;

import org.reactivestreams.*;

import io.reactivex.*;
import io.reactivex.exceptions.*;
import io.reactivex.functions.BiConsumer;
import io.reactivex.internal.fuseable.ConditionalSubscriber;
import io.reactivex.internal.subscribers.*;
import io.reactivex.plugins.RxJavaPlugins;

/**
 * Filters and/or maps source values via a callback and emitter.
 *
 * @param <T> the input value type
 * @param <R> the output value type
 */
final class FlowableMapFilter<T, R> extends Flowable<R> implements FlowableTransformer<T, R> {

    final Publisher<T> source;

    final BiConsumer<? super T, ? super BasicEmitter<R>> consumer;

    public FlowableMapFilter(Publisher<T> source, BiConsumer<? super T, ? super BasicEmitter<R>> consumer) {
        this.source = source;
        this.consumer = consumer;
    }

    @Override
    public Publisher<R> apply(Flowable<T> upstream) {
        return new FlowableMapFilter<T, R>(upstream, consumer);
    }

    @Override
    protected void subscribeActual(Subscriber<? super R> s) {
        if (s instanceof ConditionalSubscriber) {
            source.subscribe(new MapFilterConditionalSubscriber<T, R>((ConditionalSubscriber<? super R>)s, consumer));
        } else {
            source.subscribe(new MapFilterSubscriber<T, R>(s, consumer));
        }
    }

    static final class MapFilterSubscriber<T, R> extends BasicFuseableSubscriber<T, R> implements ConditionalSubscriber<T>, BasicEmitter<R> {

        final BiConsumer<? super T, ? super BasicEmitter<R>> consumer;

        boolean onNextCalled;

        R outValue;

        Throwable outError;

        public MapFilterSubscriber(Subscriber<? super R> actual,
                BiConsumer<? super T, ? super BasicEmitter<R>> consumer) {
            super(actual);
            this.consumer = consumer;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (sourceMode != NONE) {
                actual.onNext(null);
                return true;
            }
            boolean b;
            try {
                consumer.accept(t, this);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                Throwable e = outError;
                outError = null;
                if (e != null) {
                    actual.onError(new CompositeException(e, ex));
                } else {
                    actual.onError(ex);
                }
                return true;
            }
            b = onNextCalled;
            onNextCalled = false;

            if (b) {
                R v = outValue;
                outValue = null;
                actual.onNext(v);
            }
            if (done) {
                Throwable e = outError;
                outError = null;
                if (e != null) {
                    actual.onError(e);
                } else {
                    actual.onComplete();
                }
                return true;
            }

            return b;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                done = true;
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void doNext(R t) {
            if (onNextCalled) {
                doError(new IllegalStateException("doNext already called"));
            } else {
                outValue = t;
                onNextCalled = true;
            }
        }

        @Override
        public void doError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                s.cancel();
                done = true;
                outError = t;
            }
        }

        @Override
        public void doComplete() {
            if (!done) {
                s.cancel();
                done = true;
            }
        }

        @Override
        public R poll() throws Exception {
            for (;;) {
                T v = qs.poll();

                if (v == null) {
                    if (done) {
                        Throwable ex = outError;
                        outError = null;
                        if (ex != null) {
                            MapFilterSubscriber.<RuntimeException>justThrow(ex);
                        }
                    }
                    return null;
                }

                consumer.accept(v, this);

                boolean b = onNextCalled;
                onNextCalled = false;

                if (b) {
                    R o = outValue;
                    outValue = null;
                    return o;
                }

                if (done) {
                    Throwable ex = outError;
                    outError = null;
                    if (ex != null) {
                        MapFilterSubscriber.<RuntimeException>justThrow(ex);
                    }
                    return null;
                }

                if (sourceMode != SYNC) {
                    s.request(1);
                }
            }
        }

        @SuppressWarnings("unchecked")
        static <E extends Throwable> void justThrow(Throwable ex) throws E {
            throw (E)ex;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }
    }

    static final class MapFilterConditionalSubscriber<T, R> extends BasicFuseableConditionalSubscriber<T, R> implements ConditionalSubscriber<T>, BasicEmitter<R> {

        final BiConsumer<? super T, ? super BasicEmitter<R>> consumer;

        boolean onNextCalled;

        R outValue;

        Throwable outError;

        public MapFilterConditionalSubscriber(ConditionalSubscriber<? super R> actual,
                BiConsumer<? super T, ? super BasicEmitter<R>> consumer) {
            super(actual);
            this.consumer = consumer;
        }

        @Override
        public void onNext(T t) {
            if (!tryOnNext(t)) {
                s.request(1);
            }
        }

        @Override
        public boolean tryOnNext(T t) {
            if (sourceMode != NONE) {
                return actual.tryOnNext(null);
            }
            boolean b;
            try {
                consumer.accept(t, this);
            } catch (Throwable ex) {
                Exceptions.throwIfFatal(ex);
                s.cancel();
                Throwable e = outError;
                outError = null;
                if (e != null) {
                    actual.onError(new CompositeException(e, ex));
                } else {
                    actual.onError(ex);
                }
                return true;
            }
            b = onNextCalled;
            onNextCalled = false;

            if (b) {
                R v = outValue;
                outValue = null;
                b = actual.tryOnNext(v);
            }
            if (done) {
                Throwable e = outError;
                outError = null;
                if (e != null) {
                    actual.onError(e);
                } else {
                    actual.onComplete();
                }
                return true;
            }

            return b;
        }

        @Override
        public void onError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                done = true;
                actual.onError(t);
            }
        }

        @Override
        public void onComplete() {
            if (!done) {
                done = true;
                actual.onComplete();
            }
        }

        @Override
        public void request(long n) {
            s.request(n);
        }

        @Override
        public void cancel() {
            s.cancel();
        }

        @Override
        public void doNext(R t) {
            if (onNextCalled) {
                doError(new IllegalStateException("doNext already called"));
            } else {
                outValue = t;
                onNextCalled = true;
            }
        }

        @Override
        public void doError(Throwable t) {
            if (done) {
                RxJavaPlugins.onError(t);
            } else {
                s.cancel();
                done = true;
                outError = t;
            }
        }

        @Override
        public void doComplete() {
            if (!done) {
                s.cancel();
                done = true;
            }
        }

        @Override
        public R poll() throws Exception {
            for (;;) {
                T v = qs.poll();

                if (v == null) {
                    if (done) {
                        Throwable ex = outError;
                        outError = null;
                        if (ex != null) {
                            MapFilterSubscriber.<RuntimeException>justThrow(ex);
                        }
                    }
                    return null;
                }

                consumer.accept(v, this);

                boolean b = onNextCalled;
                onNextCalled = false;

                if (b) {
                    R o = outValue;
                    outValue = null;
                    return o;
                }

                if (done) {
                    Throwable ex = outError;
                    outError = null;
                    if (ex != null) {
                        MapFilterSubscriber.<RuntimeException>justThrow(ex);
                    }
                    return null;
                }

                if (sourceMode != SYNC) {
                    s.request(1);
                }
            }
        }

        @SuppressWarnings("unchecked")
        static <E extends Throwable> void justThrow(Throwable ex) throws E {
            throw (E)ex;
        }

        @Override
        public int requestFusion(int mode) {
            return transitiveBoundaryFusion(mode);
        }
    }
}