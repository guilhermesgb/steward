package com.github.guilhermesgb.steward.mvi.reservation.intent;

import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;

public class ChooseTableAction {

    private final MakeReservationsViewState.CustomerChosen firstSubstate;
    private final FetchTablesViewState.SuccessFetchingTables secondSubstate;
    private final Table chosenTable;

    public ChooseTableAction(MakeReservationsViewState.CustomerChosen firstSubstate,
                             FetchTablesViewState.SuccessFetchingTables secondSubstate,
                             Table chosenTable) {
        this.firstSubstate = firstSubstate;
        this.secondSubstate = secondSubstate;
        this.chosenTable = chosenTable;
    }

    public MakeReservationsViewState.CustomerChosen getFirstSubstate() {
        return firstSubstate;
    }

    public FetchTablesViewState.SuccessFetchingTables getSecondSubstate() {
        return secondSubstate;
    }

    public Table getChosenTable() {
        return chosenTable;
    }

}
