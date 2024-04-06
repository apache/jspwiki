/*
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
 */
package org.apache.wiki.util;

import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * <h1>Synchronizer Utility Class</h1>
 *
 * <p>This utility class is designed to provide a simplified interface for
 * executing code blocks in a synchronized manner using {@link ReentrantLock}.
 * It aims to improve code readability and maintainability by abstracting
 * common locking idioms.</p>
 *
 * <h2>Usage Example:</h2>
 * <pre>
 * {@code
 * ReentrantLock lock = new ReentrantLock();
 * String result = Synchronizer.synchronize(lock, () -> {
 *     // Your synchronized code here
 *     return "some result";
 * });
 * }
 * </pre>
 *
 * @since 2.12.2
 */
public class Synchronizer {

    /**
     * Executes a given {@link Supplier} within a synchronized block managed by
     * a {@link ReentrantLock}.
     *
     * <p>This method acquires the lock, executes the supplier's code, and then
     * releases the lock. It is designed to replace the traditional lock idiom:</p>
     *
     * <pre>
     * @code
     * lock.lock();
     * try {
     *     doSomething();
     * } finally {
     *     lock.unlock();
     * }
     * </pre>
     *
     * <p><strong>Parameters:</strong></p>
     * <ul>
     *     <li>{@code lock} - The ReentrantLock to be used for synchronization.</li>
     *     <li>{@code supplier} - The supplier whose code needs to be executed within
     *     the synchronized block.</li>
     * </ul>
     *
     * <p><strong>Returns:</strong></p>
     * <p>The result produced by the supplier.</p>
     *
     * <p><strong>Throws:</strong></p>
     * <p>This method propagates any exceptions thrown by the supplier's code.</p>
     *
     * @param <T>      The type of result produced by the supplier.
     * @param lock     The ReentrantLock to be used for synchronization.
     * @param supplier The supplier to be executed within the synchronized block.
     * @return The result produced by the supplier.
     */
    public static <T> T synchronize(final ReentrantLock lock, final Supplier<T> supplier) {
        lock.lock();
        try {
            return supplier.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * <p>Functional interface for runnable tasks that can throw exceptions.</p>
     *
     * @param <E> the type of exception that may be thrown
     */
    @FunctionalInterface
    public interface ThrowingRunnable<E extends Exception> {
        /**
         * Executes the operation.
         *
         * @throws E if an exception occurs during the operation
         */
        void run() throws E;
    }

    /**
     * <p>Throws the given exception as an unchecked exception.</p>
     *
     * @param <E>       the type of exception to throw
     * @param exception the exception to throw
     * @throws E the thrown exception
     */
    @SuppressWarnings("unchecked")
    private static < E extends Throwable > void throwAsUnchecked( final Exception exception ) throws E {
        throw (E) exception;
    }

    /**
     * <p>Executes a given {@link ThrowingRunnable} within a synchronized block managed by
     * a {@link ReentrantLock}.</p>
     *
     * <p><strong>Parameters:</strong></p>
     * <ul>
     *     <li>{@code lock} - The ReentrantLock to be used for synchronization.</li>
     *     <li>{@code throwingRunnable} - The ThrowingRunnable whose code needs to be executed within
     *     the synchronized block.</li>
     * </ul>
     *
     * <p><strong>Throws:</strong></p>
     * <p>This method propagates any exceptions thrown by the ThrowingRunnable's code.</p>
     *
     * @param <E>              the type of exception that may be thrown
     * @param lock             the ReentrantLock to use for synchronization
     * @param throwingRunnable the ThrowingRunnable to execute
     */
    public static < E extends Exception > void synchronize( final ReentrantLock lock, final ThrowingRunnable<E> throwingRunnable ) {
        lock.lock();
        try {
            throwingRunnable.run();
        } catch( final Exception e ) {
            throwAsUnchecked( e );
        } finally {
            lock.unlock();
        }
    }

}

