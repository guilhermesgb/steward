package com.github.guilhermesgb.steward.utils;

import android.support.annotation.NonNull;

import com.hannesdorfmann.mosby3.mvi.MviBasePresenter;

public abstract class OnReadyPresenter<V extends OnReadyView, VS>
        extends MviBasePresenter<V, VS> {

    @Override
    public void attachView(@NonNull V view) {
        super.attachView(view);
        view.onReady();
    }

}
