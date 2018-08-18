package com.github.guilhermesgb.steward.mvi.reservation.intent;

import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;

public class ConfirmReservationAction {

    private final MakeReservationsViewState.TableChosen finalSubstate;

    public ConfirmReservationAction(MakeReservationsViewState.TableChosen finalSubstate) {
        this.finalSubstate = finalSubstate;
    }

    public MakeReservationsViewState.TableChosen getFinalSubstate() {
        return finalSubstate;
    }

}
