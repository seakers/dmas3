/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package seakers.orekit.parallel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Integrated method to use a central thread pool
 *
 * @author nozomihitomi
 */
public class ParallelRoutine {

    /**
     * Create a singleton instance
     */
    private static ParallelRoutine instance;

    private static ExecutorService executor;

    private static ExecutorCompletionService<SubRoutine> ecs;

    /**
     * Used to prevent new task submissions when running routines
     */
    private static ReentrantLock lock;

    /**
     * The number of threads in the thread pool
     */
    private static int numThreads;

    static {
        lock = new ReentrantLock();
    }

    /**
     * Creates a singleton instance
     *
     * @param numThreads
     */
    private ParallelRoutine(int numThreads) {
        ParallelRoutine.executor = Executors.newFixedThreadPool(numThreads);
        ParallelRoutine.ecs = new ExecutorCompletionService<>(ParallelRoutine.executor);
        ParallelRoutine.numThreads = numThreads;
        instance = this;
    }

    /**
     * Gets an instance of the ParallelRoutine. A new instance is created if the
     * current thread pool has been shutdown or if the number of threads does
     * not match the current number. When creating a new instance, the current
     * pool will be shutdown and will not wait for any running processes to
     * finish
     *
     * @param numThreads The number of threads to create in the pool
     * @return an instance of ParallelRoutine
     */
    public static ParallelRoutine getInstance(int numThreads) {
        if (instance == null) {
            return new ParallelRoutine(numThreads);
        }
        if (ParallelRoutine.executor.isShutdown() || ParallelRoutine.executor.isTerminated()) {
            return new ParallelRoutine(numThreads);
        }
        if (numThreads != ParallelRoutine.numThreads) {
            ParallelRoutine.shutDown();
            return new ParallelRoutine(numThreads);
        }
        return instance;
    }

    /**
     * Submits a subroutine to the thread pool. New tasks cannot be submitted
     * when subroutines are running. If a subroutine is currently running a new
     * task is submitted, this method will return null and not run the newly
     * submitted task.
     *
     * @param subroutine routine to run or call
     * @return the completed subroutine
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public static SubRoutine submit(SubRoutine subroutine) throws InterruptedException, ExecutionException {
        if(ParallelRoutine.executor.isShutdown()){
            throw new IllegalStateException("Executor has already been shutdown and cannot accept any futher tasks until it is reset.");
        }
        
        if (ParallelRoutine.lock.isLocked()) {
            return null;
        }

        ParallelRoutine.lock.lock();
        ParallelRoutine.ecs.submit(subroutine);
        SubRoutine out = ParallelRoutine.ecs.take().get();
        ParallelRoutine.lock.unlock();
        return out;
    }

    /**
     * Submits a collection of subroutines to the thread pool. New tasks cannot
     * be submitted when subroutines are running. If a subroutine is currently
     * running a new task is submitted, this method will return null and not run
     * the newly submitted task.
     *
     * @param subroutines routine to run or call
     * @return a collection of Futures representing pending completion of the
     * tasks
     * @throws java.lang.InterruptedException
     * @throws java.util.concurrent.ExecutionException
     */
    public static Collection<SubRoutine> submit(Collection<SubRoutine> subroutines) throws InterruptedException, ExecutionException {
        if(ParallelRoutine.executor.isShutdown()){
            throw new IllegalStateException("Executor has already been shutdown and cannot accept any futher tasks until it is reset.");
        }
        
        if (ParallelRoutine.lock.isLocked()) {
            return null;
        }
        ParallelRoutine.lock.lock();
        int i = 0;
        for (SubRoutine sr : subroutines) {
            ParallelRoutine.ecs.submit(sr);
            i++;
        }
        ArrayList<SubRoutine> completedTasks = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            completedTasks.add(ParallelRoutine.ecs.take().get());
        }
        ParallelRoutine.lock.unlock();
        return completedTasks;
    }


    public static Collection<SubRoutine> blockingSubmit(Collection<SubRoutine> subroutines) throws InterruptedException, ExecutionException {
        if(ParallelRoutine.executor.isShutdown()){
            throw new IllegalStateException("Executor has already been shutdown and cannot accept any futher tasks until it is reset.");
        }

        ParallelRoutine.lock.lock();
        int i = 0;
        for (SubRoutine sr : subroutines) {
            ParallelRoutine.ecs.submit(sr);
            i++;
        }
        ArrayList<SubRoutine> completedTasks = new ArrayList<>(i);
        for (int j = 0; j < i; j++) {
            completedTasks.add(ParallelRoutine.ecs.take().get());
        }
        ParallelRoutine.lock.unlock();
        return completedTasks;
    }

    /**
     * Shuts down the thread pool and will not wait for any running processes to
     * finish
     */
    public static void shutDown() {
        ParallelRoutine.executor.shutdown();
    }

    /**
     * Checks to see if the pool is locked due to running tasks. While locked,
     * no new tasks can be submitted
     *
     * @return true if locked. Else false.
     */
    public static boolean isLocked() {
        return ParallelRoutine.lock.isLocked();
    }
}
