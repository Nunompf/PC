import java.util.concurrent.atomic.AtomicInteger;

public class UnsafeMessageBox<M> {

    private class MsgHolder {
         private final M msg;
         private final AtomicInteger lives;

        public MsgHolder(M msg, int lives){
            this.msg = msg;
            this.lives = new AtomicInteger(lives);
        }
    }

    private MsgHolder msgHolder = null;

    public void publish(M m, int lvs) {
        msgHolder = new MsgHolder(m, lvs);
    }
    public M tryConsume() {
        if (msgHolder == null) return null;
        int observedUnits;
        do {
            observedUnits = msgHolder.lives.get();
            if (observedUnits == 0) return null;
        } while (!msgHolder.lives.compareAndSet(observedUnits, observedUnits - 1));
        return msgHolder.msg;
    }

}
