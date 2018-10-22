import Utils.Timeouts;
import sun.invoke.empty.Empty;

import java.util.LinkedList;
import java.util.Optional;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageQueue<T> {

    private Lock monitor = new ReentrantLock();


    LinkedList<Message> messages = new LinkedList<>();
    LinkedList<ConsumerThreads> consumers = new LinkedList<>();

    //ver se existem threads à espera de consumir a mensagem
    public SendStatus send(T sentMsg){
        Message msg = new Message(sentMsg);
        try {
            messages.add(msg);
            if (!consumers.isEmpty()) consumers.getFirst().con.signal();
            return msg;
        }finally {
            msg.await(1000);
            monitor.unlock();
        }
    }

    //se não existem threads consumidoras a espera, se sim, passar à primeira da lista
    public Optional<T> receive(int timeout) throws InterruptedException{
        try {
            monitor.lock();
            ConsumerThreads ct = new ConsumerThreads<>(monitor.newCondition());
            long limit = Timeouts.start(timeout);
            while (true) {
                if (!messages.isEmpty()) {
                    Message m = messages.pop();
                    m.condition.signal();
                    ct.message = m;
                    m.isSent = true;
                    return Optional.of(m.message);
                } else {
                    consumers.add(ct);
                    ct.con.await();
                }
                long remainingInMs = Timeouts.remaining(limit);
                if (Timeouts.isTimeout(remainingInMs)) {
                    consumers.remove(ct);
                    return Optional.empty();
                }
            }
        }finally {
            monitor.unlock();
        }
    }

    public interface SendStatus {
        boolean isSent();
        boolean tryCancel();
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
        public boolean tryCancel() {
            if(!isSent) {
                messages.remove(this);
                return true;
            }
            return false;
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

    public class ConsumerThreads<T>{
        T message;
        Condition con;
        public ConsumerThreads(Condition con){
            this.con = con;
        }
    }
}
