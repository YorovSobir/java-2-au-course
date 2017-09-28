package ru.spbau.mit.java2;

import org.junit.*;
import org.junit.runners.MethodSorters;
import ru.spbau.mit.java2.api.ThreadPool;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
public class ThreadPoolTest {

    private static final int CORES = Runtime.getRuntime().availableProcessors();
    private static volatile ThreadPool threadPool = new ThreadPoolImpl(CORES);

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
        Assert.assertEquals(threadPoolThreadsCount(), threadPool.activeThread());
    }

    @Test
    public void testSubmit() {
        List<Supplier<Integer>> suppliers = new ArrayList<>();
        for (int i = 0; i < 2 * CORES; ++i) {
            int finalI = i;
            suppliers.add(() -> finalI);
        }
        List<LightFuture<Integer>> futures = new ArrayList<>(suppliers.size());
        suppliers.forEach(s -> futures.add(threadPool.submit(s)));

        List<Integer> results = futures
                .stream()
                .map(LightFuture::get)
                .collect(Collectors.toList());
        Assert.assertEquals(suppliers.stream().map(Supplier::get).collect(Collectors.toList()), results);
    }

    @Test
    public void testThenApply() {
        final int counter = 10;
        LightFuture<Integer> future = threadPool.submit(() -> 0);
        for (int i = 0; i < counter; ++i) {
            future = future.thenApply(x -> x + 1);
        }
        Assert.assertEquals(Integer.valueOf(counter), future.get());
    }

    @Test(expected = LightExecutionException.class)
    public void testGetException() {
        LightFuture<Integer> future = threadPool.submit(null);
        future.get();
    }

    @Test
    public void testReady() {

        LightFuture<Integer> future = threadPool.submit(() -> {
            final long bigNumber = 1_000_000_000;
            int rez = 0;
            for (int i = 0; i < bigNumber; ++i) {
                rez += i;
            }
            return rez;
        });
        Assert.assertEquals(false, future.isReady());
    }

    // this strange name for method using only for follow order in which tests will be execute
    @Test
    public void theLastTestShutdown() {
        threadPool.shutdown();
        Assert.assertEquals(0, threadPoolThreadsCount());
    }
}
