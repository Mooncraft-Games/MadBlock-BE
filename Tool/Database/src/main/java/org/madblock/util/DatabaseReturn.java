package org.madblock.util;

import java.util.function.Consumer;

/**
 * An Optional-like object which also offers further context
 * to why a database action has succeeded or failed.
 * @param <T>
 */
public final class DatabaseReturn<T> {

    private final T obj;
    private final DatabaseResult status;

    private DatabaseReturn(T obj, DatabaseResult status) {
        this.obj = obj;
        this.status = status;
    }


    public T get() {
        return this.obj;
    }

    public T getOrDefault(T defaultObj) {
        return this.isPresent() ? this.obj : defaultObj;
    }

    public DatabaseResult getStatus() {
        return this.status;
    }


    public boolean isPresent() {
        return obj != null;
    }

    public DatabaseReturn<T> ifPresent(Consumer<T> run) {
        if(this.isPresent()) run.accept(obj);
        return this;
    }

    public DatabaseReturn<T> ifNotPresent(Consumer<DatabaseResult> run) {
        if(!this.isPresent()) run.accept(this.status);
        return this;
    }


    public static <T> DatabaseReturn<T> of(T obj, DatabaseResult status) {
        return new DatabaseReturn<>(obj, status);
    }

    public static <T> DatabaseReturn<T> empty(DatabaseResult reason) {
        return new DatabaseReturn<>(null, reason);
    }
}
