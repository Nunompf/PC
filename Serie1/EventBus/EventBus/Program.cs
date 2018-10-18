
using System;
using System.Collections.Generic;
using System.Threading;

namespace EventBus
{
    public class Eventos<T>
    {
        Type t;
        static Dictionary<Type, LinkedList<Action<T>>> events = new Dictionary<Type, LinkedList<Action<T>>>();
        int maxPending;
        public Eventos(Type t, Action<T> handler, int maxPending)
        {
            this.t = t;
            this.maxPending = maxPending;
        }

        public Dictionary<Type, LinkedList<Action<T>>> GetDictionary()
        {
            return events;
        }

        public void Compute(Type key, Action<T> handler)
        {
            events.Add(key, new LinkedList<Action<T>>());
            events[typeof(T)].AddLast(handler);
        }

        public void Add(Type key, Action<T> handler)
        {
            if (events[typeof(T)].Count < maxPending)
                events[typeof(T)].AddLast(handler);
        }
    }
    public class EventBus
    {
        int maxPending;
        Object monitor = new object();

        public EventBus(int maxPending)
        {
            this.maxPending = maxPending;
        }
        public void SubscribeEvent<T>(Action<T> handler) where T : class
        {
            Eventos<T> eventos = new Eventos<T>(typeof(T), handler, maxPending);
            try
            {
                lock (monitor)
                {
                    while (true)
                    {
                        if (!eventos.GetDictionary().ContainsKey(typeof(T)))
                        {
                            eventos.Compute(typeof(T), handler);
                        }
                        else
                        {
                            if(eventos.GetDictionary()[typeof(T)].Count < maxPending)
                                eventos.Add(typeof(T), handler);
                        }
                        SyncUtils.Wait(monitor, monitor);
                    }
                }
            }
            finally
            {
                throw new ThreadInterruptedException();
            }
        }
        public void PublishEvent<E>(E message) where E : class
        {

        }
        public void Shutdown()
        {

        }
    }
}
