package com.microsoft.identity.common.internal.util;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import net.jcip.annotations.NotThreadSafe;

import java.io.File;
import java.io.IOException;
import java.util.AbstractQueue;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * A circular buffer class, designed for compact bounded-size storage.  This class internally
 * maintains an array of items with a head and a tail pointer, adding to the head and removing from
 * the tail.  In terms of synchronization, the base class offers none, but a synchronized view is
 * available by calling asSynchronized().  This class violates the contract for queue in one
 * important way: <strong>if the queue is full, add or offer will overwrite the head of the queue.</strong>
 * It is not thread safe, though the default iterator method takes a safe-copy of the backing
 * array.  If an unsafe version that does not copy is desired, it is provided as well.
 * If thread-safety is desired, a synchronized version is available by using (@link asSyncrhonized()};
 * @param <T> the type of buffered items.
 */
@NotThreadSafe
public class CircularBuffer<T> extends AbstractQueue<T> {
    private final T [] array;
    private int head;
    private int size;

    /**
     * Create a new CircularBuffer using a provided collection.
     * @param bufferBase A collection to use for the buffer.
     */
    public CircularBuffer(final @NonNull Collection<T> bufferBase, final @NonNull T[] arrayRef) {
        super();
        array = (T[]) bufferBase.toArray(arrayRef);
        head = 0;
        size = bufferBase.size();
    }

    /**
     * Create a new CircularBuffer using a provided array.
     * @param buffer the array to use for the buffer.
     */
    public CircularBuffer(final @NonNull T [] buffer) {
        super();
        array = buffer;
        head = 0;
        size = 0;
    }

    private CircularBuffer(final @NonNull T [] buffer, final int head, final int size) {
        super();
        array = buffer;
        this.head = head;
        this.size = size;
    }

    /**
     * Construct a new CircularBuffer given a size.
     * @param size The desired number of elements in the buffer.
     * @param buffer a reference to a buffer for type inference.
     */
    public CircularBuffer(final int size, final @NonNull T[] buffer) {
        array = Arrays.copyOf(buffer, size);
        head = 0;
        this.size = 0;
    }

    @Override
    public Iterator<T> iterator() {
        return Arrays.asList(toArray(Arrays.copyOf(this.array, size))).iterator();
    }

    public Iterator<T> unsafeIterator() {
        return new Iterator<T>() {
           int pos = head;
           @Override public boolean hasNext() {
               return pos - head < size;
           }
           @Override public T next() {
               if (!hasNext()) {
                   throw new NoSuchElementException();
               }
               return array[pos++ % array.length];
           }
        };
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public Object[] toArray() {
        return toArray(new Object[size]);
    }

    @Override
    public <T1> T1[] toArray(final @Nullable T1[] a) {
        if (a == null) {
            return (T1[]) toArray();
        }
        final T1[] dest;
        if (a.length < size) {
            dest = a;
        } else {
            dest = Arrays.copyOf(a, size);
        }
        for (int i = 0; i < size; i++) {
            dest[i] = (T1) array[(head + i) % array.length];
        }
        return dest;
    }

    @Override
    public boolean offer(final T t) {
        if (size >= array.length) {
            head = (head + 1) % array.length;
        } else {
            size ++;
        }
        array[(head + size - 1)%array.length] = t;
        return true;
    }

    @Override
    public T poll() {
        if (size > 0) {
            T val = array[head];
            head = (head + 1) % array.length;
            size--;
            return val;
        }
        return null;
    }

    @Override
    public T peek() {
        return size < 1 ? null : array[head];
    }

    @Override
    public void clear() {
        size = 0;
    }

    /**
     * @return a synchronized view of this CircularBuffer.
     */
    public CircularBuffer<T> asSynchronized() {
        final CircularBuffer<T> delegate = this;
        return new CircularBuffer<T>(array, head, size) {
            @Override
            public synchronized Iterator<T> iterator() {
                return delegate.iterator();
            }
            @Override
            public synchronized T peek() {
                return delegate.peek();
            }
            @Override
            public synchronized void clear() {
                delegate.clear();
            }

            @Override
            public synchronized boolean addAll(final Collection<? extends T> c) {
                return delegate.addAll(c);
            }

            @Override
            public synchronized boolean removeAll(final Collection<?> c) {
                return delegate.removeAll(c);
            }

            @Override
            public synchronized boolean retainAll(final Collection<?> c) {
                return delegate.retainAll(c);
            }

            @Override
            public synchronized int size() {
                return delegate.size();
            }

            @Override
            public synchronized Object[] toArray() {
                return delegate.toArray();
            }

            @Override
            public synchronized <T1> T1[] toArray(T1[] a) {
                return delegate.toArray(a);
            }

            @Override
            public synchronized boolean containsAll(final Collection<?> c) {
                return delegate.containsAll(c);
            }

            @Override
            public synchronized boolean offer(final T t) {
                return delegate.offer(t);
            }

            @Override
            public synchronized T poll() {
                return delegate.poll();
            }
        };
    }
}
