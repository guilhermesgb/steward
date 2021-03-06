package com.github.guilhermesgb.steward.mvi.customer.view;

import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.hannesdorfmann.mosby3.mvp.MvpView;

import io.reactivex.Observable;

public interface FetchCustomersView extends MvpView {

    Observable<FetchCustomersAction> fetchCustomersIntent();

}
