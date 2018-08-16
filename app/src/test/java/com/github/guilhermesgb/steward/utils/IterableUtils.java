package com.github.guilhermesgb.steward.utils;

public class IterableUtils<T> {

    public void forEach(Iterable<T> iterable, IterableCallback<T> callback) {
        for (T item : iterable) {
            callback.doForEach(item);
        }
    }

    public interface IterableCallback<T> {

        void doForEach(T each);

    }

}
