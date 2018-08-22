package com.github.guilhermesgb.steward.utils;

public class Argument {

    private final String key;
    private final ArgumentType type;

    public Argument(String key, ArgumentType type) {
        this.key = key;
        this.type = type;
    }

    String getKey() {
        return key;
    }

    protected ArgumentType getType() {
        return type;
    }

}
