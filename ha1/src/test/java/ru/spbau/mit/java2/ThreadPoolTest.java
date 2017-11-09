package ru.spbau.mit.java2;

import org.junit.*;
import ru.spbau.mit.java2.api.ThreadPool;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ThreadPoolTest {

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static volatile ThreadPool threadPool = new ThreadPoolImpl(CORES);

    private static ThreadPool getThreadPool() {
        if (threadPool.isShutdown()) {
            threadPool = new ThreadPoolImpl(CORES);
        }
        return threadPool;
    }

    private static long threadPoolThreadsCount() {
        ThreadGroup currentGroup = Thread.currentThread().getThreadGroup();
        int noThreads = currentGroup.activeCount();
        Thread[] lstThreads = new Thread[noThreads];
        currentGroup.enumerate(lstThreads);
        return Arrays.stream(lstThreads)
                .filter(t -> t.getName().matches("threadPool: thread-[0-9]+"))
                .count();
    }

    @Test
    public void testRunningThread() {
        Assert.assertEquals(CORES, threadPoolThreadsCount());
    }

    @Test
    public void testSubmit() {
        ThreadPool curThreadPool = getThreadPool();
        List<Supplier<Integer>> suppliers = new ArrayList<>();
        for (int i = 0; i < 2 * CORES; ++i) {
            int finalI = i;
            suppliers.add(() -> finalI);
        }
        List<LightFuture<Integer>> futures = new ArrayList<>(suppliers.size());
        suppliers.forEach(s -> futures.add(curThreadPool.submit(s)));

        List<Integer> results = futures
                .stream()
                .map(LightFuture::get)
                .collect(Collectors.toList());
        Assert.assertEquals(suppliers.stream().map(Supplier::get).collect(Collectors.toList()), results);
    }

    @Test
    public void testThenApply() {
        ThreadPool curThreadPool = getThreadPool();
        final int counter = 10;
        LightFuture<Integer> future = curThreadPool.submit(() -> 0);
        for (int i = 0; i < counter; ++i) {
            future = future.thenApply(x -> x + 1);
        }
        Assert.assertEquals(Integer.valueOf(counter), future.get());
    }

    @Test(expected = LightExecutionException.class)
    public void testGetException() {
        ThreadPool curThreadPool = getThreadPool();
        LightFuture<Integer> future = curThreadPool.submit(null);
        future.get();
    }

    @Test
    public void testReady() {
        ThreadPool curThreadPool = getThreadPool();
        LightFuture<Integer> future = curThreadPool.submit(() -> {
            final long bigNumber = 1_000_000_000;
            int rez = 0;
            for (int i = 0; i < bigNumber; ++i) {
                rez += i;
            }
            return rez;
        });
        Assert.assertEquals(false, future.isReady());
    }

    @Test
    public void testShutdown() {
        ThreadPool curThreadPool = getThreadPool();
        curThreadPool.shutdown();
        Assert.assertEquals(0, threadPoolThreadsCount());
    }

    @Test(timeout = 2000)
    public void testThenApplyLongFirstTask() {
        ThreadPool curThreadPool = getThreadPool();

        LightFuture<Integer> future = curThreadPool.submit(() -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) { }
            return 0;

        });
        for (int i = 0; i < CORES; ++i) {
            future = future.thenApply(x -> {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) { }
                return x + 1;
            });
        }
        LightFuture<Integer> lastFuture = curThreadPool.submit(() -> 1);
        Assert.assertEquals(1, lastFuture.get().intValue());
    }
}
