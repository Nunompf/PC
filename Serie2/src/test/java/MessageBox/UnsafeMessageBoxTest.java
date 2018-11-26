package MessageBox;

import MessageBox.UnsafeMessageBox;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

import static org.junit.Assert.*;

public class UnsafeMessageBoxTest {

    UnsafeMessageBox<String> mb = new UnsafeMessageBox<String>();

    @Test
    public void tryConsume() throws InterruptedException {
        int lives = 10;
        int numOfThreads = 10;

        mb.publish("Message", lives);

        Thread[] threads = new Thread[numOfThreads];
        List<String> results = new LinkedList<>();

        for(int i = 0; i < threads.length; i++){
            threads[i] = new Thread(() -> {
                results.add(mb.tryConsume());
            });
            threads[i].start();
        }
        for(Thread th: threads)
            th.join();

        long count = results.stream().filter(Objects::nonNull).count();

        assertEquals(lives, count);
    }
}