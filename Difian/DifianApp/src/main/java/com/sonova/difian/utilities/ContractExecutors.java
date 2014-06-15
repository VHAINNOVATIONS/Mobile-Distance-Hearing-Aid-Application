// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.utilities;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * ContractExecutors, that propagates uncaught Throwables, especially Errors or RuntimeExceptions
 */
public final class ContractExecutors
{

    private static ThreadFactory contractThreadFactorySingleton;

    private ContractExecutors()
    {
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class ContractThreadFactory implements ThreadFactory
    {
        private ContractThreadFactory()
        {
        }

        @Override
        public Thread newThread(Runnable runnable)
        {
            Thread thread = new Thread(runnable);
            final Thread.UncaughtExceptionHandler uncaughtExceptionHandler = thread.getUncaughtExceptionHandler();
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
            {
                @SuppressWarnings("AnonymousClassVariableHidesContainingMethodVariable")
                @Override
                public void uncaughtException(Thread thread, Throwable throwable)
                {
                    if (throwable != null)
                    {
                        uncaughtExceptionHandler.uncaughtException(thread, throwable);
                    }
                }
            });
            return thread;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static ThreadFactory defaultThreadFactory()
    {
        if (contractThreadFactorySingleton == null)
        {
            contractThreadFactorySingleton = new ContractThreadFactory();
        }
        return contractThreadFactorySingleton;
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class ContractThreadPoolExecutor extends ThreadPoolExecutor
    {

        public ContractThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, ThreadFactory threadFactory)
        {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable throwable)
        {
            super.afterExecute(r, throwable);
            if ((throwable == null) && (r instanceof Future))
            {
                try
                {
                    ((Future)r).get();
                }
                catch (CancellationException ce)
                {
                    throwable = null;
                }
                catch (ExecutionException ee)
                {
                    throwable = ee.getCause();
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt(); // ignore/reset
                }
            }
            if (throwable != null)
            {
                if (throwable instanceof Error)
                {
                    //noinspection ProhibitedExceptionThrown
                    throw (Error)throwable;
                }
                else if (throwable instanceof RuntimeException)
                {
                    //noinspection ProhibitedExceptionThrown
                    throw (RuntimeException)throwable;
                }
                else
                {
                    //noinspection ProhibitedExceptionThrown
                    throw new RuntimeException(throwable);
                }
            }
        }
    }

    public static ExecutorService newSingleThreadExecutor()
    {
        return new ContractThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), defaultThreadFactory());
    }

    public static ExecutorService newCachedThreadPool()
    {
        return new ContractThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), defaultThreadFactory());
    }

    public static ExecutorService newFixedThreadPool(int nThreads)
    {
        return new ContractThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), defaultThreadFactory());
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class ContractScheduledThreadPoolExecutor extends ScheduledThreadPoolExecutor
    {

        public ContractScheduledThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory)
        {
            super(corePoolSize, threadFactory);
        }

        @Override
        protected void afterExecute(Runnable r, Throwable throwable)
        {
            super.afterExecute(r, throwable);
            if ((throwable == null) && (r instanceof Future))
            {
                try
                {
                    ((Future)r).get();
                }
                catch (CancellationException ce)
                {
                    throwable = null;
                }
                catch (ExecutionException ee)
                {
                    throwable = ee.getCause();
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt(); // ignore/reset
                }
            }
            if (throwable != null)
            {
                if (throwable instanceof Error)
                {
                    //noinspection ProhibitedExceptionThrown
                    throw (Error)throwable;
                }
                else if (throwable instanceof RuntimeException)
                {
                    //noinspection ProhibitedExceptionThrown
                    throw (RuntimeException)throwable;
                }
                else
                {
                    //noinspection ProhibitedExceptionThrown
                    throw new RuntimeException(throwable);
                }
            }
        }
    }

    public static ScheduledExecutorService newScheduledThreadPool(int corePoolSize)
    {
        return new ContractScheduledThreadPoolExecutor(corePoolSize, defaultThreadFactory());
    }
}
