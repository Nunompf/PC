import Utils.Timeouts;

import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class KeyedExchanger<T> {

    private T data;
    private Lock monitor;
    private HashMap<Integer, T> exchanger = new HashMap<>();
    private Condition condition;

    public KeyedExchanger(){
        monitor = new ReentrantLock();
        condition = monitor.newCondition();
    }

    public Optional<T> exchange(int ky, T mydata, int timeout) throws InterruptedException{
        try{
            monitor.lock();
            if(!exchanger.containsKey(ky)){
                exchanger.put(ky, mydata);
            }
            else{
                T auxData = exchanger.get(ky);
                exchanger.put(ky, mydata);
                condition.signal();
                return Optional.of(auxData);
            }
            long limit = Timeouts.start(timeout);
            while (true){
                try {
                    condition.await(timeout, TimeUnit.MILLISECONDS);
                }catch(InterruptedException e){
                    checkAndGo(exchanger, ky, mydata);
                }

                if(!exchanger.get(ky).equals(mydata)){
                    T value = exchanger.get(ky);
                    exchanger.remove(ky);
                    return Optional.of(value);
                }
                long remainingInMs = Timeouts.remaining(limit);
                if(Timeouts.isTimeout(remainingInMs)){
                    return checkAndGo(exchanger, ky, mydata);
                }
            }
        }finally {
            monitor.unlock();
        }
    }
    private Optional<T> checkAndGo(HashMap<Integer, T> exchanger, int ky, T mydata){
        T auxData;
        if(exchanger.containsKey(ky) && !exchanger.get(ky).equals(mydata)) {
            auxData = exchanger.get(ky);
            exchanger.remove(ky);
            return Optional.of(auxData);
        }
        exchanger.remove(ky);
        return Optional.empty();
    }
}
