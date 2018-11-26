package MessageQueue;

import org.junit.Test;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.Integer.*;
import static org.junit.Assert.*;

public class MessageQueueTest {

    static int xorShift(int y) {
        y ^= (y << 6);
        y ^= (y >>> 21);
        y ^= (y << 7);
        return y;
    }
    @Test
    public void test() {
        final ExecutorService pool = Executors.newCachedThreadPool();
        final AtomicInteger putSum = new AtomicInteger(0);
        final AtomicInteger takeSum = new AtomicInteger(0);
        final CyclicBarrier barrier;
        final MessageQueue<Integer> mq = new MessageQueue<Integer>();
        final int nTrials, nPairs;
        nTrials = 10000;
        nPairs = 10;
        barrier = new CyclicBarrier(nPairs * 2 + 1);
        try {
            for (int i = 0; i < nPairs; i++) {
                pool.execute(() -> {
                    try {
                        int seed = (this.hashCode() ^ (int) System.nanoTime());;
                        int sum = 0;
                        barrier.await();
                        for (int j = nTrials; j > 0; --j) {
                            mq.send(seed);
                            sum += seed;
                            seed = xorShift(seed);
                        }
                        putSum.getAndAdd(sum);
                        barrier.await();
                    }catch(Exception e){
                        throw new RuntimeException(e);
                    }
                });
                pool.execute(() -> {
                    try {
                        barrier.await();
                        int sum = 0;
                        for (int k = nTrials; k > 0; --k) {
                            sum += mq.receive(MAX_VALUE).get();
                        }
                        takeSum.getAndAdd(sum);
                        barrier.await();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            }
            barrier.await(); // wait for all threads to be ready
            barrier.await(); // wait for all threads to finish
            assertEquals(putSum.get(), takeSum.get());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}