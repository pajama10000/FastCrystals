// 
// Decompiled by Procyon v0.5.36
// 

package xyz.fxcilities.core.collections.expiringmap;

import java.util.concurrent.ConcurrentSkipListSet;
import java.util.SortedSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.concurrent.Future;
import java.util.NoSuchElementException;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.Objects;
import java.util.AbstractSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import xyz.fxcilities.core.collections.expiringmap.internal.NamedThreadFactory;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import xyz.fxcilities.core.Checks;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicLong;
import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ConcurrentMap;

public class ExpiringMap<K, V> implements ConcurrentMap<K, V>
{
    static volatile ScheduledExecutorService EXPIRER;
    static volatile ThreadPoolExecutor LISTENER_SERVICE;
    static ThreadFactory THREAD_FACTORY;
    List<ExpirationListener<K, V>> expirationListeners;
    List<ExpirationListener<K, V>> asyncExpirationListeners;
    private AtomicLong expirationNanos;
    private int maxSize;
    private final AtomicReference<ExpirationPolicy> expirationPolicy;
    private final EntryLoader<? super K, ? extends V> entryLoader;
    private final ExpiringEntryLoader<? super K, ? extends V> expiringEntryLoader;
    private final ReadWriteLock readWriteLock;
    private final Lock readLock;
    private final Lock writeLock;
    private final EntryMap<K, V> entries;
    private final boolean variableExpiration;
    
    public static void setThreadFactory(final ThreadFactory threadFactory) {
        ExpiringMap.THREAD_FACTORY = Checks.nonNull(threadFactory, "threadFactory");
    }
    
    private ExpiringMap(final Builder<K, V> builder) {
        this.readWriteLock = new ReentrantReadWriteLock();
        this.readLock = this.readWriteLock.readLock();
        this.writeLock = this.readWriteLock.writeLock();
        if (ExpiringMap.EXPIRER == null) {
            synchronized (ExpiringMap.class) {
                if (ExpiringMap.EXPIRER == null) {
                    ExpiringMap.EXPIRER = Executors.newSingleThreadScheduledExecutor((ExpiringMap.THREAD_FACTORY == null) ? new NamedThreadFactory("ExpiringMap-Expirer") : ExpiringMap.THREAD_FACTORY);
                }
            }
        }
        if (ExpiringMap.LISTENER_SERVICE == null && builder.asyncExpirationListeners != null) {
            this.initListenerService();
        }
        this.variableExpiration = builder.variableExpiration;
        this.entries = (EntryMap<K, V>)(this.variableExpiration ? new EntryTreeHashMap<Object, Object>() : new EntryLinkedHashMap<Object, Object>());
        if (builder.expirationListeners != null) {
            this.expirationListeners = new CopyOnWriteArrayList<ExpirationListener<K, V>>(builder.expirationListeners);
        }
        if (builder.asyncExpirationListeners != null) {
            this.asyncExpirationListeners = new CopyOnWriteArrayList<ExpirationListener<K, V>>(builder.asyncExpirationListeners);
        }
        this.expirationPolicy = new AtomicReference<ExpirationPolicy>(builder.expirationPolicy);
        this.expirationNanos = new AtomicLong(TimeUnit.NANOSECONDS.convert(builder.duration, builder.timeUnit));
        this.maxSize = builder.maxSize;
        this.entryLoader = (EntryLoader<? super K, ? extends V>)builder.entryLoader;
        this.expiringEntryLoader = (ExpiringEntryLoader<? super K, ? extends V>)builder.expiringEntryLoader;
    }
    
    public static Builder<Object, Object> builder() {
        return new Builder<Object, Object>();
    }
    
    public static <K, V> ExpiringMap<K, V> create() {
        return new ExpiringMap<K, V>((Builder<K, V>)builder());
    }
    
    public synchronized void addExpirationListener(final ExpirationListener<K, V> listener) {
        Checks.nonNull(listener, "listener");
        if (this.expirationListeners == null) {
            this.expirationListeners = new CopyOnWriteArrayList<ExpirationListener<K, V>>();
        }
        this.expirationListeners.add(listener);
    }
    
    public synchronized void addAsyncExpirationListener(final ExpirationListener<K, V> listener) {
        Checks.nonNull(listener, "listener");
        if (this.asyncExpirationListeners == null) {
            this.asyncExpirationListeners = new CopyOnWriteArrayList<ExpirationListener<K, V>>();
        }
        this.asyncExpirationListeners.add(listener);
        if (ExpiringMap.LISTENER_SERVICE == null) {
            this.initListenerService();
        }
    }
    
    @Override
    public void clear() {
        this.writeLock.lock();
        try {
            for (final ExpiringEntry<K, V> entry : this.entries.values()) {
                entry.cancel();
            }
            this.entries.clear();
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    @Override
    public boolean containsKey(final Object key) {
        this.readLock.lock();
        try {
            return this.entries.containsKey(key);
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public boolean containsValue(final Object value) {
        this.readLock.lock();
        try {
            return this.entries.containsValue(value);
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return new AbstractSet<Map.Entry<K, V>>() {
            @Override
            public void clear() {
                ExpiringMap.this.clear();
            }
            
            @Override
            public boolean contains(final Object entry) {
                if (!(entry instanceof Map.Entry)) {
                    return false;
                }
                final Map.Entry<?, ?> e = (Map.Entry<?, ?>)entry;
                return ExpiringMap.this.containsKey(e.getKey());
            }
            
            @Override
            public Iterator<Map.Entry<K, V>> iterator() {
                Object o;
                if (ExpiringMap.this.entries instanceof EntryLinkedHashMap) {
                    final EntryLinkedHashMap entryLinkedHashMap;
                    o = entryLinkedHashMap.new EntryIterator();
                    entryLinkedHashMap = (EntryLinkedHashMap)ExpiringMap.this.entries;
                    Objects.requireNonNull(entryLinkedHashMap);
                }
                else {
                    final EntryTreeHashMap entryTreeHashMap;
                    o = entryTreeHashMap.new EntryIterator();
                    entryTreeHashMap = (EntryTreeHashMap)ExpiringMap.this.entries;
                    Objects.requireNonNull(entryTreeHashMap);
                }
                return (Iterator<Map.Entry<K, V>>)o;
            }
            
            @Override
            public boolean remove(final Object entry) {
                if (entry instanceof Map.Entry) {
                    final Map.Entry<?, ?> e = (Map.Entry<?, ?>)entry;
                    return ExpiringMap.this.remove(e.getKey()) != null;
                }
                return false;
            }
            
            @Override
            public int size() {
                return ExpiringMap.this.size();
            }
        };
    }
    
    @Override
    public boolean equals(final Object obj) {
        this.readLock.lock();
        try {
            return this.entries.equals(obj);
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public V get(final Object key) {
        final ExpiringEntry<K, V> entry = this.getEntry(key);
        if (entry == null) {
            return this.load(key);
        }
        if (ExpirationPolicy.ACCESSED.equals(entry.expirationPolicy.get())) {
            this.resetEntry(entry, false);
        }
        return entry.getValue();
    }
    
    private V load(final K key) {
        if (this.entryLoader == null && this.expiringEntryLoader == null) {
            return null;
        }
        this.writeLock.lock();
        try {
            final ExpiringEntry<K, V> entry = this.getEntry(key);
            if (entry != null) {
                return entry.getValue();
            }
            if (this.entryLoader != null) {
                final V value = (V)this.entryLoader.load((Object)key);
                this.put(key, value);
                return value;
            }
            final ExpiringValue<? extends V> expiringValue = this.expiringEntryLoader.load((Object)key);
            if (expiringValue == null) {
                this.put(key, null);
                return null;
            }
            final long duration = (expiringValue.getTimeUnit() == null) ? this.expirationNanos.get() : expiringValue.getDuration();
            final TimeUnit timeUnit = (expiringValue.getTimeUnit() == null) ? TimeUnit.NANOSECONDS : expiringValue.getTimeUnit();
            this.put(key, expiringValue.getValue(), (expiringValue.getExpirationPolicy() == null) ? this.expirationPolicy.get() : expiringValue.getExpirationPolicy(), duration, timeUnit);
            return (V)expiringValue.getValue();
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    public long getExpiration() {
        return TimeUnit.NANOSECONDS.toMillis(this.expirationNanos.get());
    }
    
    public long getExpiration(final K key) {
        Checks.nonNull(key, "key");
        final ExpiringEntry<K, V> entry = this.getEntry(key);
        Checks.element(entry, key);
        return TimeUnit.NANOSECONDS.toMillis(entry.expirationNanos.get());
    }
    
    public ExpirationPolicy getExpirationPolicy(final K key) {
        Checks.nonNull(key, "key");
        final ExpiringEntry<K, V> entry = this.getEntry(key);
        Checks.element(entry, key);
        return entry.expirationPolicy.get();
    }
    
    public long getExpectedExpiration(final K key) {
        Checks.nonNull(key, "key");
        final ExpiringEntry<K, V> entry = this.getEntry(key);
        Checks.element(entry, key);
        return TimeUnit.NANOSECONDS.toMillis(entry.expectedExpiration.get() - System.nanoTime());
    }
    
    public int getMaxSize() {
        return this.maxSize;
    }
    
    @Override
    public int hashCode() {
        this.readLock.lock();
        try {
            return this.entries.hashCode();
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public boolean isEmpty() {
        this.readLock.lock();
        try {
            return this.entries.isEmpty();
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public Set<K> keySet() {
        return new AbstractSet<K>() {
            @Override
            public void clear() {
                ExpiringMap.this.clear();
            }
            
            @Override
            public boolean contains(final Object key) {
                return ExpiringMap.this.containsKey(key);
            }
            
            @Override
            public Iterator<K> iterator() {
                Object o;
                if (ExpiringMap.this.entries instanceof EntryLinkedHashMap) {
                    final EntryLinkedHashMap entryLinkedHashMap;
                    o = entryLinkedHashMap.new KeyIterator();
                    entryLinkedHashMap = (EntryLinkedHashMap)ExpiringMap.this.entries;
                    Objects.requireNonNull(entryLinkedHashMap);
                }
                else {
                    final EntryTreeHashMap entryTreeHashMap;
                    o = entryTreeHashMap.new KeyIterator();
                    entryTreeHashMap = (EntryTreeHashMap)ExpiringMap.this.entries;
                    Objects.requireNonNull(entryTreeHashMap);
                }
                return (Iterator<K>)o;
            }
            
            @Override
            public boolean remove(final Object value) {
                return ExpiringMap.this.remove(value) != null;
            }
            
            @Override
            public int size() {
                return ExpiringMap.this.size();
            }
        };
    }
    
    @Override
    public V put(final K key, final V value) {
        Checks.nonNull(key, "key");
        return this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
    }
    
    public V put(final K key, final V value, final ExpirationPolicy expirationPolicy) {
        return this.put(key, value, expirationPolicy, this.expirationNanos.get(), TimeUnit.NANOSECONDS);
    }
    
    public V put(final K key, final V value, final long duration, final TimeUnit timeUnit) {
        return this.put(key, value, this.expirationPolicy.get(), duration, timeUnit);
    }
    
    public V put(final K key, final V value, final ExpirationPolicy expirationPolicy, final long duration, final TimeUnit timeUnit) {
        Checks.nonNull(key, "key");
        Checks.nonNull(expirationPolicy, "expirationPolicy");
        Checks.nonNull(timeUnit, "timeUnit");
        Checks.check(this.variableExpiration, "Variable expiration is not enabled");
        return this.putInternal(key, value, expirationPolicy, TimeUnit.NANOSECONDS.convert(duration, timeUnit));
    }
    
    @Override
    public void putAll(final Map<? extends K, ? extends V> map) {
        Checks.nonNull(map, "map");
        final long expiration = this.expirationNanos.get();
        final ExpirationPolicy expirationPolicy = this.expirationPolicy.get();
        this.writeLock.lock();
        try {
            for (final Map.Entry<? extends K, ? extends V> entry : map.entrySet()) {
                this.putInternal(entry.getKey(), entry.getValue(), expirationPolicy, expiration);
            }
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    @Override
    public V putIfAbsent(final K key, final V value) {
        Checks.nonNull(key, "key");
        this.writeLock.lock();
        try {
            if (!this.entries.containsKey(key)) {
                return (V)this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
            }
            return (V)this.entries.get(key).getValue();
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    @Override
    public V remove(final Object key) {
        Checks.nonNull(key, "key");
        this.writeLock.lock();
        try {
            final ExpiringEntry<K, V> entry = this.entries.remove(key);
            if (entry == null) {
                return null;
            }
            if (entry.cancel()) {
                this.scheduleEntry(this.entries.first());
            }
            return entry.getValue();
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    @Override
    public boolean remove(final Object key, final Object value) {
        Checks.nonNull(key, "key");
        this.writeLock.lock();
        try {
            final ExpiringEntry<K, V> entry = this.entries.get(key);
            if (entry != null && entry.getValue().equals(value)) {
                this.entries.remove(key);
                if (entry.cancel()) {
                    this.scheduleEntry(this.entries.first());
                }
                return true;
            }
            return false;
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    @Override
    public V replace(final K key, final V value) {
        Checks.nonNull(key, "key");
        this.writeLock.lock();
        try {
            if (this.entries.containsKey(key)) {
                return (V)this.putInternal(key, value, this.expirationPolicy.get(), this.expirationNanos.get());
            }
            return null;
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    @Override
    public boolean replace(final K key, final V oldValue, final V newValue) {
        Checks.nonNull(key, "key");
        this.writeLock.lock();
        try {
            final ExpiringEntry<K, V> entry = this.entries.get(key);
            if (entry != null && entry.getValue().equals(oldValue)) {
                this.putInternal(key, newValue, this.expirationPolicy.get(), this.expirationNanos.get());
                return true;
            }
            return false;
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    public void removeExpirationListener(final ExpirationListener<K, V> listener) {
        Checks.nonNull(listener, "listener");
        for (int i = 0; i < this.expirationListeners.size(); ++i) {
            if (this.expirationListeners.get(i).equals(listener)) {
                this.expirationListeners.remove(i);
                return;
            }
        }
    }
    
    public void removeAsyncExpirationListener(final ExpirationListener<K, V> listener) {
        Checks.nonNull(listener, "listener");
        for (int i = 0; i < this.asyncExpirationListeners.size(); ++i) {
            if (this.asyncExpirationListeners.get(i).equals(listener)) {
                this.asyncExpirationListeners.remove(i);
                return;
            }
        }
    }
    
    public void resetExpiration(final K key) {
        Checks.nonNull(key, "key");
        final ExpiringEntry<K, V> entry = this.getEntry(key);
        if (entry != null) {
            this.resetEntry(entry, false);
        }
    }
    
    public void setExpiration(final K key, final long duration, final TimeUnit timeUnit) {
        Checks.nonNull(key, "key");
        Checks.nonNull(timeUnit, "timeUnit");
        Checks.check(this.variableExpiration, "Variable expiration is not enabled");
        this.writeLock.lock();
        try {
            final ExpiringEntry<K, V> entry = this.entries.get(key);
            if (entry != null) {
                entry.expirationNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
                this.resetEntry(entry, true);
            }
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    public void setExpiration(final long duration, final TimeUnit timeUnit) {
        Checks.nonNull(timeUnit, "timeUnit");
        Checks.check(this.variableExpiration, "Variable expiration is not enabled");
        this.expirationNanos.set(TimeUnit.NANOSECONDS.convert(duration, timeUnit));
    }
    
    public void setExpirationPolicy(final ExpirationPolicy expirationPolicy) {
        Checks.nonNull(expirationPolicy, "expirationPolicy");
        this.expirationPolicy.set(expirationPolicy);
    }
    
    public void setExpirationPolicy(final K key, final ExpirationPolicy expirationPolicy) {
        Checks.nonNull(key, "key");
        Checks.nonNull(expirationPolicy, "expirationPolicy");
        Checks.check(this.variableExpiration, "Variable expiration is not enabled");
        final ExpiringEntry<K, V> entry = this.getEntry(key);
        if (entry != null) {
            entry.expirationPolicy.set(expirationPolicy);
        }
    }
    
    public void setMaxSize(final int maxSize) {
        Checks.check(maxSize > 0, "maxSize");
        this.maxSize = maxSize;
    }
    
    @Override
    public int size() {
        this.readLock.lock();
        try {
            return this.entries.size();
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public String toString() {
        this.readLock.lock();
        try {
            return this.entries.toString();
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public void clear() {
                ExpiringMap.this.clear();
            }
            
            @Override
            public boolean contains(final Object value) {
                return ExpiringMap.this.containsValue(value);
            }
            
            @Override
            public Iterator<V> iterator() {
                Object o;
                if (ExpiringMap.this.entries instanceof EntryLinkedHashMap) {
                    final EntryLinkedHashMap entryLinkedHashMap;
                    o = entryLinkedHashMap.new ValueIterator();
                    entryLinkedHashMap = (EntryLinkedHashMap)ExpiringMap.this.entries;
                    Objects.requireNonNull(entryLinkedHashMap);
                }
                else {
                    final EntryTreeHashMap entryTreeHashMap;
                    o = entryTreeHashMap.new ValueIterator();
                    entryTreeHashMap = (EntryTreeHashMap)ExpiringMap.this.entries;
                    Objects.requireNonNull(entryTreeHashMap);
                }
                return (Iterator<V>)o;
            }
            
            @Override
            public int size() {
                return ExpiringMap.this.size();
            }
        };
    }
    
    void notifyListeners(final ExpiringEntry<K, V> entry) {
        if (this.asyncExpirationListeners != null) {
            for (final ExpirationListener<K, V> listener : this.asyncExpirationListeners) {
                ExpiringMap.LISTENER_SERVICE.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            listener.expired(entry.key, entry.getValue());
                        }
                        catch (Exception ex) {}
                    }
                });
            }
        }
        if (this.expirationListeners != null) {
            for (final ExpirationListener<K, V> listener : this.expirationListeners) {
                try {
                    listener.expired(entry.key, entry.getValue());
                }
                catch (Exception ex) {}
            }
        }
    }
    
    ExpiringEntry<K, V> getEntry(final Object key) {
        this.readLock.lock();
        try {
            return this.entries.get(key);
        }
        finally {
            this.readLock.unlock();
        }
    }
    
    V putInternal(final K key, final V value, final ExpirationPolicy expirationPolicy, final long expirationNanos) {
        this.writeLock.lock();
        try {
            ExpiringEntry<K, V> entry = this.entries.get(key);
            V oldValue = null;
            if (entry == null) {
                entry = new ExpiringEntry<K, V>(key, value, this.variableExpiration ? new AtomicReference<ExpirationPolicy>(expirationPolicy) : this.expirationPolicy, this.variableExpiration ? new AtomicLong(expirationNanos) : this.expirationNanos);
                if (this.entries.size() >= this.maxSize) {
                    final ExpiringEntry<K, V> expiredEntry = this.entries.first();
                    this.entries.remove(expiredEntry.key);
                    this.notifyListeners(expiredEntry);
                }
                this.entries.put(key, entry);
                if (this.entries.size() == 1 || this.entries.first().equals(entry)) {
                    this.scheduleEntry(entry);
                }
            }
            else {
                oldValue = entry.getValue();
                if (!ExpirationPolicy.ACCESSED.equals(expirationPolicy) && ((oldValue == null && value == null) || (oldValue != null && oldValue.equals(value)))) {
                    return value;
                }
                entry.setValue(value);
                this.resetEntry(entry, false);
            }
            return oldValue;
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    void resetEntry(final ExpiringEntry<K, V> entry, final boolean scheduleFirstEntry) {
        this.writeLock.lock();
        try {
            final boolean scheduled = entry.cancel();
            this.entries.reorder(entry);
            if (scheduled || scheduleFirstEntry) {
                this.scheduleEntry(this.entries.first());
            }
        }
        finally {
            this.writeLock.unlock();
        }
    }
    
    void scheduleEntry(final ExpiringEntry<K, V> entry) {
        if (entry == null || entry.scheduled) {
            return;
        }
        Runnable runnable = null;
        synchronized (entry) {
            if (entry.scheduled) {
                return;
            }
            final WeakReference<ExpiringEntry<K, V>> entryReference = new WeakReference<ExpiringEntry<K, V>>(entry);
            runnable = new Runnable() {
                @Override
                public void run() {
                    final ExpiringEntry<K, V> entry = (ExpiringEntry<K, V>)entryReference.get();
                    ExpiringMap.this.writeLock.lock();
                    try {
                        if (entry != null && entry.scheduled) {
                            ExpiringMap.this.entries.remove(entry.key);
                            ExpiringMap.this.notifyListeners(entry);
                        }
                        try {
                            final Iterator<ExpiringEntry<K, V>> iterator = ExpiringMap.this.entries.valuesIterator();
                            boolean schedulePending = true;
                            while (iterator.hasNext() && schedulePending) {
                                final ExpiringEntry<K, V> nextEntry = iterator.next();
                                if (nextEntry.expectedExpiration.get() <= System.nanoTime()) {
                                    iterator.remove();
                                    ExpiringMap.this.notifyListeners(nextEntry);
                                }
                                else {
                                    ExpiringMap.this.scheduleEntry(nextEntry);
                                    schedulePending = false;
                                }
                            }
                        }
                        catch (NoSuchElementException ex) {}
                    }
                    finally {
                        ExpiringMap.this.writeLock.unlock();
                    }
                }
            };
            final Future<?> entryFuture = ExpiringMap.EXPIRER.schedule(runnable, entry.expectedExpiration.get() - System.nanoTime(), TimeUnit.NANOSECONDS);
            entry.schedule(entryFuture);
        }
    }
    
    private static <K, V> Map.Entry<K, V> mapEntryFor(final ExpiringEntry<K, V> entry) {
        return new Map.Entry<K, V>() {
            @Override
            public K getKey() {
                return entry.key;
            }
            
            @Override
            public V getValue() {
                return entry.value;
            }
            
            @Override
            public V setValue(final V value) {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    private void initListenerService() {
        synchronized (ExpiringMap.class) {
            if (ExpiringMap.LISTENER_SERVICE == null) {
                ExpiringMap.LISTENER_SERVICE = (ThreadPoolExecutor)Executors.newCachedThreadPool((ExpiringMap.THREAD_FACTORY == null) ? new NamedThreadFactory("ExpiringMap-Listener-%s") : ExpiringMap.THREAD_FACTORY);
            }
        }
    }
    
    public static final class Builder<K, V>
    {
        private ExpirationPolicy expirationPolicy;
        private List<ExpirationListener<K, V>> expirationListeners;
        private List<ExpirationListener<K, V>> asyncExpirationListeners;
        private TimeUnit timeUnit;
        private boolean variableExpiration;
        private long duration;
        private int maxSize;
        private EntryLoader<K, V> entryLoader;
        private ExpiringEntryLoader<K, V> expiringEntryLoader;
        
        private Builder() {
            this.expirationPolicy = ExpirationPolicy.CREATED;
            this.timeUnit = TimeUnit.SECONDS;
            this.duration = 60L;
            this.maxSize = Integer.MAX_VALUE;
        }
        
        public <K1 extends K, V1 extends V> ExpiringMap<K1, V1> build() {
            return new ExpiringMap<K1, V1>((Builder<K1, V1>)this);
        }
        
        public Builder<K, V> expiration(final long duration, final TimeUnit timeUnit) {
            this.duration = duration;
            this.timeUnit = Checks.nonNull(timeUnit, "timeUnit");
            return this;
        }
        
        public Builder<K, V> maxSize(final int maxSize) {
            Checks.check(maxSize > 0, "maxSize");
            this.maxSize = maxSize;
            return this;
        }
        
        public <K1 extends K, V1 extends V> Builder<K1, V1> entryLoader(final EntryLoader<? super K1, ? super V1> loader) {
            this.assertNoLoaderSet();
            this.entryLoader = Checks.nonNull(loader, "loader");
            return (Builder<K1, V1>)this;
        }
        
        public <K1 extends K, V1 extends V> Builder<K1, V1> expiringEntryLoader(final ExpiringEntryLoader<? super K1, ? super V1> loader) {
            this.assertNoLoaderSet();
            this.expiringEntryLoader = Checks.nonNull(loader, "loader");
            this.variableExpiration();
            return (Builder<K1, V1>)this;
        }
        
        public <K1 extends K, V1 extends V> Builder<K1, V1> expirationListener(final ExpirationListener<? super K1, ? super V1> listener) {
            Checks.nonNull(listener, "listener");
            if (this.expirationListeners == null) {
                this.expirationListeners = new ArrayList<ExpirationListener<K, V>>();
            }
            this.expirationListeners.add((ExpirationListener<K, V>)listener);
            return (Builder<K1, V1>)this;
        }
        
        public <K1 extends K, V1 extends V> Builder<K1, V1> expirationListeners(final List<ExpirationListener<? super K1, ? super V1>> listeners) {
            Checks.nonNull(listeners, "listeners");
            if (this.expirationListeners == null) {
                this.expirationListeners = new ArrayList<ExpirationListener<K, V>>(listeners.size());
            }
            for (final ExpirationListener<? super K1, ? super V1> listener : listeners) {
                this.expirationListeners.add((ExpirationListener<K, V>)listener);
            }
            return (Builder<K1, V1>)this;
        }
        
        public <K1 extends K, V1 extends V> Builder<K1, V1> asyncExpirationListener(final ExpirationListener<? super K1, ? super V1> listener) {
            Checks.nonNull(listener, "listener");
            if (this.asyncExpirationListeners == null) {
                this.asyncExpirationListeners = new ArrayList<ExpirationListener<K, V>>();
            }
            this.asyncExpirationListeners.add((ExpirationListener<K, V>)listener);
            return (Builder<K1, V1>)this;
        }
        
        public <K1 extends K, V1 extends V> Builder<K1, V1> asyncExpirationListeners(final List<ExpirationListener<? super K1, ? super V1>> listeners) {
            Checks.nonNull(listeners, "listeners");
            if (this.asyncExpirationListeners == null) {
                this.asyncExpirationListeners = new ArrayList<ExpirationListener<K, V>>(listeners.size());
            }
            for (final ExpirationListener<? super K1, ? super V1> listener : listeners) {
                this.asyncExpirationListeners.add((ExpirationListener<K, V>)listener);
            }
            return (Builder<K1, V1>)this;
        }
        
        public Builder<K, V> expirationPolicy(final ExpirationPolicy expirationPolicy) {
            this.expirationPolicy = Checks.nonNull(expirationPolicy, "expirationPolicy");
            return this;
        }
        
        public Builder<K, V> variableExpiration() {
            this.variableExpiration = true;
            return this;
        }
        
        private void assertNoLoaderSet() {
            Checks.state(this.entryLoader == null && this.expiringEntryLoader == null, "Either entryLoader or expiringEntryLoader may be set, not both", new Object[0]);
        }
    }
    
    private static class EntryLinkedHashMap<K, V> extends LinkedHashMap<K, ExpiringEntry<K, V>> implements EntryMap<K, V>
    {
        private static final long serialVersionUID = 1L;
        
        @Override
        public boolean containsValue(final Object value) {
            for (final ExpiringEntry<K, V> entry : this.values()) {
                final V v = entry.value;
                if (v == value || (value != null && value.equals(v))) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public ExpiringEntry<K, V> first() {
            return this.isEmpty() ? null : this.values().iterator().next();
        }
        
        @Override
        public void reorder(final ExpiringEntry<K, V> value) {
            this.remove(value.key);
            value.resetExpiration();
            this.put(value.key, value);
        }
        
        @Override
        public Iterator<ExpiringEntry<K, V>> valuesIterator() {
            return this.values().iterator();
        }
        
        abstract class AbstractHashIterator
        {
            private final Iterator<Map.Entry<K, ExpiringEntry<K, V>>> iterator;
            private ExpiringEntry<K, V> next;
            
            AbstractHashIterator() {
                this.iterator = (Iterator<Map.Entry<K, ExpiringEntry<K, V>>>)EntryLinkedHashMap.this.entrySet().iterator();
            }
            
            public boolean hasNext() {
                return this.iterator.hasNext();
            }
            
            public ExpiringEntry<K, V> getNext() {
                return this.next = this.iterator.next().getValue();
            }
            
            public void remove() {
                this.iterator.remove();
            }
        }
        
        final class KeyIterator extends AbstractHashIterator implements Iterator<K>
        {
            @Override
            public final K next() {
                return this.getNext().key;
            }
        }
        
        final class ValueIterator extends AbstractHashIterator implements Iterator<V>
        {
            @Override
            public final V next() {
                return this.getNext().value;
            }
        }
        
        public final class EntryIterator extends AbstractHashIterator implements Iterator<Map.Entry<K, V>>
        {
            @Override
            public final Map.Entry<K, V> next() {
                return ExpiringMap.mapEntryFor((ExpiringEntry<K, V>)this.getNext());
            }
        }
    }
    
    private static class EntryTreeHashMap<K, V> extends HashMap<K, ExpiringEntry<K, V>> implements EntryMap<K, V>
    {
        private static final long serialVersionUID = 1L;
        SortedSet<ExpiringEntry<K, V>> sortedSet;
        
        private EntryTreeHashMap() {
            this.sortedSet = new ConcurrentSkipListSet<ExpiringEntry<K, V>>();
        }
        
        @Override
        public void clear() {
            super.clear();
            this.sortedSet.clear();
        }
        
        @Override
        public boolean containsValue(final Object value) {
            for (final ExpiringEntry<K, V> entry : this.values()) {
                final V v = entry.value;
                if (v == value || (value != null && value.equals(v))) {
                    return true;
                }
            }
            return false;
        }
        
        @Override
        public ExpiringEntry<K, V> first() {
            return this.sortedSet.isEmpty() ? null : this.sortedSet.first();
        }
        
        @Override
        public ExpiringEntry<K, V> put(final K key, final ExpiringEntry<K, V> value) {
            this.sortedSet.add(value);
            return super.put(key, value);
        }
        
        @Override
        public ExpiringEntry<K, V> remove(final Object key) {
            final ExpiringEntry<K, V> entry = super.remove(key);
            if (entry != null) {
                this.sortedSet.remove(entry);
            }
            return entry;
        }
        
        @Override
        public void reorder(final ExpiringEntry<K, V> value) {
            this.sortedSet.remove(value);
            value.resetExpiration();
            this.sortedSet.add(value);
        }
        
        @Override
        public Iterator<ExpiringEntry<K, V>> valuesIterator() {
            return new ExpiringEntryIterator();
        }
        
        abstract class AbstractHashIterator
        {
            private final Iterator<ExpiringEntry<K, V>> iterator;
            protected ExpiringEntry<K, V> next;
            
            AbstractHashIterator() {
                this.iterator = EntryTreeHashMap.this.sortedSet.iterator();
            }
            
            public boolean hasNext() {
                return this.iterator.hasNext();
            }
            
            public ExpiringEntry<K, V> getNext() {
                return this.next = this.iterator.next();
            }
            
            public void remove() {
                HashMap.this.remove(this.next.key);
                this.iterator.remove();
            }
        }
        
        final class ExpiringEntryIterator extends AbstractHashIterator implements Iterator<ExpiringEntry<K, V>>
        {
            @Override
            public final ExpiringEntry<K, V> next() {
                return this.getNext();
            }
        }
        
        final class KeyIterator extends AbstractHashIterator implements Iterator<K>
        {
            @Override
            public final K next() {
                return this.getNext().key;
            }
        }
        
        final class ValueIterator extends AbstractHashIterator implements Iterator<V>
        {
            @Override
            public final V next() {
                return this.getNext().value;
            }
        }
        
        final class EntryIterator extends AbstractHashIterator implements Iterator<Map.Entry<K, V>>
        {
            @Override
            public final Map.Entry<K, V> next() {
                return ExpiringMap.mapEntryFor(this.getNext());
            }
        }
    }
    
    static class ExpiringEntry<K, V> implements Comparable<ExpiringEntry<K, V>>
    {
        final AtomicLong expirationNanos;
        final AtomicLong expectedExpiration;
        final AtomicReference<ExpirationPolicy> expirationPolicy;
        final K key;
        volatile Future<?> entryFuture;
        V value;
        volatile boolean scheduled;
        
        ExpiringEntry(final K key, final V value, final AtomicReference<ExpirationPolicy> expirationPolicy, final AtomicLong expirationNanos) {
            this.key = key;
            this.value = value;
            this.expirationPolicy = expirationPolicy;
            this.expirationNanos = expirationNanos;
            this.expectedExpiration = new AtomicLong();
            this.resetExpiration();
        }
        
        @Override
        public int compareTo(final ExpiringEntry<K, V> other) {
            if (this.key.equals(other.key)) {
                return 0;
            }
            return (this.expectedExpiration.get() < other.expectedExpiration.get()) ? -1 : 1;
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = 31 * result + ((this.key == null) ? 0 : this.key.hashCode());
            result = 31 * result + ((this.value == null) ? 0 : this.value.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (this.getClass() != obj.getClass()) {
                return false;
            }
            final ExpiringEntry<?, ?> other = (ExpiringEntry<?, ?>)obj;
            if (!this.key.equals(other.key)) {
                return false;
            }
            if (this.value == null) {
                if (other.value != null) {
                    return false;
                }
            }
            else if (!this.value.equals(other.value)) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return this.value.toString();
        }
        
        synchronized boolean cancel() {
            final boolean result = this.scheduled;
            if (this.entryFuture != null) {
                this.entryFuture.cancel(false);
            }
            this.entryFuture = null;
            this.scheduled = false;
            return result;
        }
        
        synchronized V getValue() {
            return this.value;
        }
        
        void resetExpiration() {
            this.expectedExpiration.set(this.expirationNanos.get() + System.nanoTime());
        }
        
        synchronized void schedule(final Future<?> entryFuture) {
            this.entryFuture = entryFuture;
            this.scheduled = true;
        }
        
        synchronized void setValue(final V value) {
            this.value = value;
        }
    }
    
    private interface EntryMap<K, V> extends Map<K, ExpiringEntry<K, V>>
    {
        ExpiringEntry<K, V> first();
        
        void reorder(final ExpiringEntry<K, V> p0);
        
        Iterator<ExpiringEntry<K, V>> valuesIterator();
    }
}
