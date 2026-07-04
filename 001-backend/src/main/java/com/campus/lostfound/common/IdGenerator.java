package com.campus.lostfound.common;

import java.util.concurrent.ThreadLocalRandom;

public final class IdGenerator {
    private IdGenerator() {
    }

    public static long nextId() {
        long millis = System.currentTimeMillis();
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return Long.parseLong(millis + String.valueOf(random));
    }
}
