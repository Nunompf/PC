
using System;
using System.Collections.Generic;
using System.Threading;

namespace EventBus
{
    public class EventBus
    {
        int maxPending;
        bool shutdown;
        bool isDone = false;
        Object monitor = new object();
        Dictionary<Type, LinkedList<Subscribers>> events = new Dictionary<Type, LinkedList<Subscribers>>();

        public EventBus(int maxPending)
        {
            this.maxPending = maxPending;
        }
        public void SubscribeEvent<T>(Action<T> handler) where T : class
        {
            Subscribers subs = new Subscribers(maxPending);
            
            lock (monitor)
            {
                while (true)
                {
                    if (shutdown) return;
                    if (!events.ContainsKey(typeof(T)))
                    {
                        events.Add(typeof(T), new LinkedList<Subscribers>());
                        events[typeof(T)].AddLast(subs);
                    }
                    else
                    {
                        if(events[typeof(T)].Count < subs.getMaxPending())
                            events[typeof(T)].AddLast(subs);
                    }
                    try
                    {
                        SyncUtils.Wait(monitor, subs.con);
                        subs.execute(handler);
                        events[typeof(T)].Remove(subs);
                        if (shutdown)
                        {
                            while(events[typeof(T)].Contains(subs));
                            events[typeof(T)].Remove(subs);
                            return;
                        }
                    }
                    catch(ThreadInterruptedException e)
                    {

                    }

                }
            }
            
            
        }
        public void PublishEvent<E>(E message) where E : class
        {
            lock (monitor)
            {
                if (shutdown)
                {
                    throw new InvalidOperationException();
                }
                foreach (var v in events)
                {
                    if (v.Key.Equals(typeof(E)))
                    {
                        foreach(Subscribers s in v.Value)
                        {
                            s.message = message;
                            SyncUtils.Notify(monitor, s.con); 
                        }
                    }
                }
            }
            
        }
        public void Shutdown()
        {
            lock (monitor)
            {
                shutdown = true;
                while (events.Keys != null)
                {
                    foreach(var v in events.Keys)
                    {
                        if (events[v].Count == 0) events.Remove(v);
                    }
                }
            }
        }
    }

    public class Subscribers
    {
        public Object message;
        public Object con = new object();

        int maxPending;
        public Subscribers(int maxPending)
        {
            this.maxPending = maxPending;
        }
        public void execute<T>(Action<T> action) => action((T)message);

        public int getMaxPending()
        {
            return maxPending;
        }
    }

}
