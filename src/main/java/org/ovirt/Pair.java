package org.ovirt;

/**
 * Created by ahino on 9/11/15.
 */
public class Pair<T, U> {

    private T first;
    private U second;

    public Pair(T t, U u) {
        first = t;
        second = u;
    }

    public T getFirst() {
        return first;
    }

    public U getSecond() {
        return  second;
    }
}
