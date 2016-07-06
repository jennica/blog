package com.ninjaspounced.blog;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test to compare locking with AtomicBoolean vs. regular synchronized methods.
 * 
 * @author jennica
 */
public class SpinlockTest
{
    /** Number of iterations used in adding up loop */
    private static final int NUM_ITERATIONS = 100000;

    /** Class members used in fake spinlock, fine grained */
    private volatile int fakeSpinlockNum1 = 0;
    private volatile int fakeSpinlockNum2 = 0;

    /** Atomic boolean used as a spinlock substitute */
    private AtomicBoolean fakeSpinLock = new AtomicBoolean(false);

    /**
     * Inner class for running a fine grained spinlock implementation
     * 
     * @author jennica
     */
    private class FakeSpinlockRunnable implements Runnable
    {
        @Override
        public void run()
        {
            for (int i = 0; i < NUM_ITERATIONS; ++i)
            {

                // This is the part which does the "locking"
                while (!fakeSpinLock.compareAndSet(false, true))
                    ;
                // Critical section
                fakeSpinlockNum1 += 1;
                fakeSpinlockNum2 += 3;

                // Release the "lock"
                fakeSpinLock.set(false);
            }
        }
    }

    /** Class members for finer grained synchronized block */
    private volatile int mutexNum1 = 0;
    private volatile int mutexNum2 = 0;

    /**
     * Fine-grained mutex implementation
     * 
     * @author jennica
     */
    private class MutexRunnable implements Runnable
    {
        @Override
        public void run()
        {
            for (int i = 0; i < NUM_ITERATIONS; ++i)
            {
                synchronized (SpinlockTest.this)
                {
                    mutexNum1 += 1;
                    mutexNum2 += 3;

                }
            }
        }
    }

    /** Coarse synchronization private members */
    private volatile int coarseMutexNum1 = 0;
    private volatile int coarseMutexNum2 = 0;

    /**
     * Coarse synchronization block
     * 
     * @author jennica
     */
    private class CoarseMutexRunnable implements Runnable
    {
        @Override
        public void run()
        {
            synchronized (SpinlockTest.this)
            {
                for (int i = 0; i < NUM_ITERATIONS; ++i)
                {
                    coarseMutexNum1 += 1;
                    coarseMutexNum2 += 3;
                }
            }
        }
    }

    /** Coarse "spinlock" private members */
    private volatile int coarseSpinlockNum1 = 0;
    private volatile int coarseSpinlockNum2 = 0;

    /**
     * Threaded implementation for incrementing numbers on a coarse "spinlock"
     * 
     * @author jennica
     */
    private class CoarseSpinlockRunnable implements Runnable
    {

        @Override
        public void run()
        {
            // Grab the "lock"
            while (!fakeSpinLock.compareAndSet(false, true))
                ;

            // Critical section
            for (int i = 0; i < 10000000; ++i)
            {
                coarseSpinlockNum1 += 1;
                coarseSpinlockNum2 += 3;
            }

            // Release the "lock"
            fakeSpinLock.set(false);
        }
    }

    @Override
    public String toString()
    {
        return "SpinlockTest [fakeSpinlockNum1=" + fakeSpinlockNum1 + ", fakeSpinlockNum2=" + fakeSpinlockNum2
                + ", fakeSpinLock=" + fakeSpinLock + ", mutexNum1=" + mutexNum1 + ", mutexNum2=" + mutexNum2
                + ", coarseMutexNum1=" + coarseMutexNum1 + ", coarseMutexNum2=" + coarseMutexNum2
                + ", coarseSpinlockNum1=" + coarseSpinlockNum1 + ", coarseSpinlockNum2=" + coarseSpinlockNum2 + "]";
    }

    /**
     * Runs spinlock test on a particular scenario given a number of threads
     * 
     * @param clazz
     *            locking mechanism to test
     * @param num_threads
     *            to use in running
     * @throws InstantiationException
     *             we're being lazy and using reflection
     * @throws IllegalAccessException
     *             we're being lazy and using reflection
     * @throws InterruptedException
     *             if something interrupts the thread, which we are counting on
     *             not happening for blog code.
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    private void runThreads(Class<? extends Runnable> clazz, final int num_threads)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
            SecurityException, InterruptedException
    {
        final Thread[] threads = new Thread[num_threads];
        for (int i = 0; i < num_threads; ++i)
        {
            Constructor<?> ctor = clazz.getDeclaredConstructors()[0];
            ctor.setAccessible(true);
            Runnable r = (Runnable) ctor.newInstance(this);
            threads[i] = new Thread(r);
        }

        final long start_time = System.currentTimeMillis();
        for (int i = 0; i < num_threads; ++i)
            threads[i].start();
        for (int i = 0; i < num_threads; ++i)
            threads[i].join();
        long elapsed = System.currentTimeMillis();
        System.out.println(clazz.getName() + " took " + (elapsed - start_time) + " msec on " + num_threads
                + " threads, " + NUM_ITERATIONS + " iterations each thread");
    }

    /**
     * Runs against all spinlock scenarios given a number of threads
     * 
     * @param num_threads
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InterruptedException
     * @throws SecurityException
     * @throws InvocationTargetException
     * @throws IllegalArgumentException
     */
    public void runSuite(final int num_threads) throws InstantiationException, IllegalAccessException,
            InterruptedException, IllegalArgumentException, InvocationTargetException, SecurityException
    {
        runThreads(MutexRunnable.class, num_threads);
        runThreads(CoarseMutexRunnable.class, num_threads);
        runThreads(FakeSpinlockRunnable.class, num_threads);
        runThreads(CoarseSpinlockRunnable.class, num_threads);
        System.out.println(toString());
    }

    public static void main(String[] args) throws InterruptedException, InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, SecurityException
    {
        SpinlockTest test = new SpinlockTest();
        for (int i = 0; i < 5; ++i)
            test.runSuite(i+1);
        /*
        for (int i = 10; i <= 60; i += 5)
            test.runSuite(i);
            */
    }
}