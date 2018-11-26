package MessageBox;

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

    private volatile MsgHolder msgHolder = null;

    public void publish(M m, int lvs) {
        msgHolder = new MsgHolder(m, lvs);
    }
    public M tryConsume() {
        MsgHolder holder = this.msgHolder;
        if (holder == null) return null;
        int observedUnits;
        do {
            observedUnits = holder.lives.get();
            if (observedUnits == 0) return null;
        } while (!holder.lives.compareAndSet(observedUnits, observedUnits - 1));
        return holder.msg;
    }

}
