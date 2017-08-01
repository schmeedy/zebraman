package com.github.schmeedy.zonky.java;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

public class CollectionUtils {

    public static <E> List<E> takeWhile(Iterable<? extends E> es, Predicate<? super E> predicate) {
        List<E> result = new LinkedList<>();
        for (E e: es) {
            if (predicate.test(e)) {
                result.add(e);
            } else {
                break;
            }
        }
        return result;
    }

}
