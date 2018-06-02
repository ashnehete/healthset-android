package in.ashnehete.healthset.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by Aashish Nehete on 04-Jan-18.
 */

public class BoundedQueueLinkedList<E> extends LinkedList<E> {

    private final int maxSize;
    private int front;

    public BoundedQueueLinkedList(int maxSize) {
        this.maxSize = maxSize;
        this.front = 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#add(java.lang.Object)
     */
    @Override
    public boolean add(E object) {
        if (size() - front == maxSize) {
//            removeFirst();
            front++;
        }
        return super.add(object);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#add(int, java.lang.Object)
     */
    @Override
    public void add(int location, E object) {
        // if (size() == maxSize) {
        //     removeFirst();
        // }
        // super.add(location, object);
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#addAll(java.util.Collection)
     */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
//        final int totalNeededSize = size() + collection.size();
//        final int overhead = totalNeededSize - maxSize;
//        if (overhead > 0) {
//            removeRange(0, overhead);
//        }
//        return super.addAll(collection);
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#addAll(int, java.util.Collection)
     */
    @Override
    public boolean addAll(int location, Collection<? extends E> collection) {
        // int totalNeededSize = size() + collection.size();
        // int overhead = totalNeededSize - maxSize;
        // if(overhead > 0) {
        // removeRange(0, overhead);
        // }
        // return super.addAll(location, collection);
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#addFirst(java.lang.Object)
     */
    @Override
    public void addFirst(E object) {
        // super.addFirst(object);
        throw new UnsupportedOperationException();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.util.LinkedList#addLast(java.lang.Object)
     */
    @Override
    public void addLast(E object) {
        add(object);
    }

    @Override
    public int size() {
        return super.size() - front;
    }

    @Override
    public E get(int index) {
        return super.get(front + index);
    }

    @Override
    public String toString() {
        StringBuffer stringBuffer = new StringBuffer();
        String separator = ",";

        Iterator<E> iterator = this.iterator();
        while (iterator.hasNext()) {
            stringBuffer.append(iterator.next().toString());
            if (iterator.hasNext()) stringBuffer.append(separator);
        }

        return stringBuffer.toString();
    }
}
