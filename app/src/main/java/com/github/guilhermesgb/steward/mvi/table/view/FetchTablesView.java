package com.github.guilhermesgb.steward.mvi.table.view;

import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.hannesdorfmann.mosby3.mvp.MvpView;

import io.reactivex.Observable;

public interface FetchTablesView extends MvpView {

    Observable<FetchTablesAction> fetchTablesIntent();

    void render(FetchTablesViewState state);

}
