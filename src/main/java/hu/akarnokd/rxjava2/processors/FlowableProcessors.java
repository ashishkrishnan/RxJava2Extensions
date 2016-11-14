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

package hu.akarnokd.rxjava2.processors;

import org.reactivestreams.Processor;

import io.reactivex.internal.functions.ObjectHelper;
import io.reactivex.processors.FlowableProcessor;

/**
 * Utility methods to work with Reactive-Streams Processors and RxJava 2 FlowableProcessors.
 */
public final class FlowableProcessors {
    /** Utility class. */
    private FlowableProcessors() {
        throw new IllegalStateException("No instances!");
    }

    /**
     * Wraps an arbitrary Reactive-Streams {@link Processor} into a {@link FlowableProcessor}, relaying
     * the onXXX and subscribe() calls to it and providing a rich fluent API on top.
     * <p>Note that RxJava 2 doesn't support a FlowableProcessor with different input
     * and output types.
     * @param <T> the input and output type
     * @param processor the processor to wrap (or return if already a FlowableProcessor), not null
     * @return the FlowableProcessor instance possible wrapping the input processor
     */
    public static <T> FlowableProcessor<T> wrap(Processor<T, T> processor) {
        if (processor instanceof FlowableProcessor) {
            return (FlowableProcessor<T>)processor;
        }
        return new FlowableProcessorWrap<T>(ObjectHelper.requireNonNull(processor, "processor is null"));
    }
}
