package com.github.guilhermesgb.steward.utils;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public abstract class ViewStateOption {

    protected  <O> void doAccept(Consumer<O> optionConsumer, O option) {
        try {
            optionConsumer.accept(option);
        } catch (Exception ignore) {}
    }

    protected <O, R> R doApply(Function<O, R> optionFunction, O option) {
        try {
            return optionFunction.apply(option);
        } catch (Exception ignore) {
            return null;
        }
    }

    @Override
    public String toString() {
        return getOptionName();
    }

    protected abstract String getOptionName();

}
