package MessageQueue;

import Utils.LockFreeQueue;
import Utils.Timeouts;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageQueue<T> {

    private final Lock monitor = new ReentrantLock();
    private LockFreeQueue<Message> freeMessages = new LockFreeQueue<>();
    private volatile int numOfConsumerThreads = 0;

    //ver se existem threads à espera de consumir a mensagem
    public SendStatus send(T sentMsg){
        Message msg = new Message(sentMsg);
        freeMessages.enqueue(msg);

        if(numOfConsumerThreads != 0){
            synchronized (monitor){
                monitor.notify();
            }
        }
        return msg;
    }

    public boolean tryReceive(){
        return freeMessages.isEmpty();
    }

    //se não existem threads consumidoras a espera, se sim, passar à primeira da lista
    public Optional<T> receive(int timeout) throws InterruptedException{
        if (!tryReceive()) {
            Message m = freeMessages.dequeue();
            try{
                monitor.lock();
                m.condition.signal();
                m.isSent = true;
                return Optional.of(m.message);
            }finally {
                monitor.unlock();
            }
        }

        synchronized (monitor) {
            long limit = Timeouts.start(timeout);
            long remaining = Timeouts.remaining(limit);
            numOfConsumerThreads++;
            while (tryReceive()) {
                try {
                    monitor.wait(remaining);
                } catch (InterruptedException e) {
                    if (!tryReceive()) {
                        Message m = freeMessages.dequeue();
                        m.condition.signal();
                        m.isSent = true;
                        Thread.currentThread().interrupt();
                        numOfConsumerThreads--;
                        return Optional.of(m.message);
                    }
                    throw e;
                }
                long remainingInMs = Timeouts.remaining(limit);
                if (Timeouts.isTimeout(remainingInMs)) {
                    numOfConsumerThreads--;
                    return Optional.empty();
                }
            }
            if(!tryReceive()){
                Message m = freeMessages.dequeue();
                try {
                    monitor.lock();
                    m.condition.signal();
                    numOfConsumerThreads--;
                    m.isSent = true;
                    return Optional.of(m.message);
                }finally {
                    monitor.unlock();
                }
            }
        }
        return Optional.empty();
    }

    public interface SendStatus {
        boolean isSent();
        boolean await(int timeout) throws InterruptedException;
    }

    public class Message implements SendStatus{

        Condition condition = monitor.newCondition();
        public T message;
        boolean isSent = false;

        public Message(T message){
            this.message = message;
        }

        @Override
        public boolean isSent() {
            return isSent;
        }

        @Override
        public boolean await(int timeout) {

            try {
                monitor.lock();
                if (isSent) return true;
                long limit = Timeouts.start(timeout);
                while (true) {
                    try {
                        condition.await();
                    } catch (InterruptedException e) {
                        return false;
                    }
                    if (isSent) return true;
                    long remainingInMs = Timeouts.remaining(limit);
                    if (Timeouts.isTimeout(remainingInMs)) {
                        return false;
                    }
                }
            }finally {
                monitor.unlock();
            }
        }
    }
}
