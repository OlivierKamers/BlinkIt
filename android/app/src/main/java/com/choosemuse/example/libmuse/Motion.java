package com.choosemuse.example.libmuse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public enum Motion {
    LEFT, RIGHT, BLINK;

    private static final List<Motion> VALUES =
            Collections.unmodifiableList(Arrays.asList(values()));
    private static final int SIZE = VALUES.size();
    private static final Random RANDOM = new Random();

    public static Motion getRandomMotion() {
        return VALUES.get(RANDOM.nextInt(SIZE));
    }
}
