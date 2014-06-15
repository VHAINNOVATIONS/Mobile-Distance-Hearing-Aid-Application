// Copyright (c) 2014 Phonak, Inc. and Oberon microsystems, Inc. All rights reserved.

package com.sonova.difian.utilities;

import android.os.Handler;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

public final class BackgroundOperations
{

    @SuppressWarnings("PublicInnerClass")
    public abstract static class BackgroundTask<T>
    {

        protected BackgroundTask()
        {
        }

        @SuppressWarnings("InstanceVariableNamingConvention")
        private Future<T> future;
        @SuppressWarnings("InstanceVariableNamingConvention")
        private Handler handler;

        private void start(ExecutorService executor)
        {
            if (!isRunning())
            {
                handler = new Handler();
                final FutureTask<T> futureTask = new FutureTask<T>(new Callable<T>()
                {
                    @SuppressWarnings("ProhibitedExceptionDeclared")
                    @Override
                    public T call() throws Exception
                    {
                        return doInBackground();
                    }
                });
                future = futureTask;
                executor.submit(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        T result;
                        futureTask.run();
                        Throwable throwable;
                        try
                        {
                            result = futureTask.get();
                            throwable = null;
                        }
                        catch (CancellationException e)
                        {
                            result = null;
                            throwable = null;
                        }
                        catch (ExecutionException e)
                        {
                            result = null;
                            throwable = e.getCause();
                        }
                        catch (InterruptedException e)
                        {
                            result = null;
                            throwable = e.getCause();
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
                        final T finalResult = result;
                        handler.post(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                if ((futureTask != null) && !futureTask.isCancelled())
                                {
                                    onCompleted(finalResult);
                                }
                            }
                        });
                    }
                });
            }
        }

        private void stop()
        {
            if (future != null)
            {
                future.cancel(false);
                future = null;
            }
        }

        private boolean isRunning()
        {
            return (future != null) && !future.isDone();
        }

        protected final void publishProgress(final T value)
        {
            handler.post(new Runnable()
            {
                @Override
                public void run()
                {
                    if ((future != null) && !future.isCancelled())
                    {
                        onProgress(value);
                    }
                }
            });
        }

        @SuppressWarnings({"RedundantThrows", "ProhibitedExceptionDeclared"})
        protected abstract T doInBackground() throws Exception;

        @SuppressWarnings({"WeakerAccess", "EmptyMethod", "UnnecessaryFinalOnLocalVariableOrParameter", "UnusedParameters"})
        protected void onProgress(final T result)
        {
        }

        @SuppressWarnings("UnnecessaryFinalOnLocalVariableOrParameter")
        protected void onCompleted(final T result)
        {
        }
    }

    @SuppressWarnings("PublicInnerClass")
    public static final class BackgroundOperation
    {

        @SuppressWarnings("InstanceVariableNamingConvention")
        private final ExecutorService executorService;

        @SuppressWarnings("InstanceVariableNamingConvention")
        private BackgroundTask backgroundTask;

        @SuppressWarnings("ParameterHidesMemberVariable")
        public BackgroundOperation(ExecutorService executorService)
        {
            this.executorService = executorService;
        }

        @SuppressWarnings("ParameterHidesMemberVariable")
        public <T> void start(@SuppressWarnings("UnnecessaryFinalOnLocalVariableOrParameter") final BackgroundTask<T> backgroundTask)
        {
            this.backgroundTask = backgroundTask;
            if (backgroundTask != null)
            {
                backgroundTask.start(executorService);
            }
        }

        public void stop()
        {
            if (backgroundTask != null)
            {
                backgroundTask.stop();
            }
        }

        public boolean isRunning()
        {
            return (backgroundTask != null) && backgroundTask.isRunning();
        }
    }
}
