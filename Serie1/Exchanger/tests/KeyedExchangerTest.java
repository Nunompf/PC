import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class KeyedExchangerTest {

    KeyedExchanger<Integer> ke = new KeyedExchanger<>();
    private Optional<Integer> test;

    @Test
    public void noExchange() throws InterruptedException{
        Thread th = new Thread(() ->{
            try{
                test = ke.exchange(1, 10, 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        th.start();
        th.join();
        assertEquals(Optional.empty(), test);
    }
    @Test
    public void twoThreadsExchange() throws InterruptedException{
        Object[] array = new Object[2];
        Thread t1 = new Thread(() -> {
            try{
                array[0] = ke.exchange(1, 10, 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        Thread t2 = new Thread(() -> {
            try {
                array[1] = ke.exchange(1, 20, 100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.start();
        t2.start();
        t1.join();
        t2.join();
        assertEquals(Optional.of(20), array[0]);
        assertEquals(Optional.of(10), array[1]);
    }
}