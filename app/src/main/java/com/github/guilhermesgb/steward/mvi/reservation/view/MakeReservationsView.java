package com.github.guilhermesgb.steward.mvi.reservation.view;

import com.github.guilhermesgb.steward.mvi.customer.view.FetchCustomersView;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseCustomerAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseTableAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ConfirmReservationAction;
import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;
import com.github.guilhermesgb.steward.mvi.table.view.FetchTablesView;
import com.github.guilhermesgb.steward.utils.OnReadyView;

import io.reactivex.Observable;

public interface MakeReservationsView extends FetchCustomersView, FetchTablesView, OnReadyView {

    Observable<ChooseCustomerAction> chooseCustomerIntent();

    Observable<ChooseTableAction> chooseTableIntent();

    Observable<ConfirmReservationAction> confirmReservationIntent();

    void render(MakeReservationsViewState state);

}
