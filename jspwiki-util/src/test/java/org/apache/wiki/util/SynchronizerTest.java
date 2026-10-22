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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

class SynchronizerTest {

    @Test
    void testSupplierReturnsValueAndReleasesLock() {
        final ReentrantLock lock = new ReentrantLock();

        final String result = Synchronizer.synchronize( lock, () -> {
            Assertions.assertTrue( lock.isHeldByCurrentThread() );
            return "value";
        } );

        Assertions.assertEquals( "value", result );
        Assertions.assertFalse( lock.isLocked() );
    }

    @Test
    void testSupplierReleasesLockWhenItThrows() {
        final ReentrantLock lock = new ReentrantLock();

        Assertions.assertThrows( IllegalStateException.class, () -> Synchronizer.synchronize( lock, () -> {
            throw new IllegalStateException( "boom" );
        } ) );

        Assertions.assertFalse( lock.isLocked() );
    }

    @Test
    void testThrowingRunnableRethrowsCheckedExceptionUnwrapped() {
        final ReentrantLock lock = new ReentrantLock();

        final Synchronizer.ThrowingRunnable< IOException > failing = () -> {
            throw new IOException( "disk" );
        };

        final IOException e = Assertions.assertThrows( IOException.class, () -> Synchronizer.synchronize( lock, failing ) );

        Assertions.assertEquals( "disk", e.getMessage() );
        Assertions.assertFalse( lock.isLocked() );
    }

    @Test
    void testLockIsReentrant() {
        final ReentrantLock lock = new ReentrantLock();

        Synchronizer.synchronize( lock, () -> Synchronizer.synchronize( lock, () -> {
            Assertions.assertEquals( 2, lock.getHoldCount() );
        } ) );

        Assertions.assertFalse( lock.isLocked() );
    }

    @Test
    void testMutualExclusion() throws InterruptedException {
        final ReentrantLock lock = new ReentrantLock();
        final List< Integer > list = new ArrayList<>();
        final int threads = 8;
        final int perThread = 2_000;
        final CountDownLatch start = new CountDownLatch( 1 );
        final List< Thread > workers = new ArrayList<>();
        for( int t = 0; t < threads; t++ ) {
            final Thread worker = new Thread( () -> {
                try {
                    start.await();
                } catch( final InterruptedException e ) {
                    Thread.currentThread().interrupt();
                    return;
                }
                for( int i = 0; i < perThread; i++ ) {
                    Synchronizer.synchronize( lock, () -> list.add( 1 ) );
                }
            } );
            workers.add( worker );
            worker.start();
        }

        start.countDown();
        for( final Thread worker : workers ) {
            worker.join();
        }

        // an unguarded ArrayList loses elements or throws under this load
        Assertions.assertEquals( threads * perThread, list.size() );
    }

}
