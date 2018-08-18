package com.github.guilhermesgb.steward.mvi.reservation.intent;

import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;

public class ChooseCustomerAction {

    private final FetchCustomersViewState.SuccessFetchingCustomers substate;
    private final Customer chosenCustomer;
    private FetchTablesAction fetchTablesAction;

    public ChooseCustomerAction(FetchCustomersViewState.SuccessFetchingCustomers substate,
                                Customer chosenCustomer) {
        this.substate = substate;
        this.chosenCustomer = chosenCustomer;
    }

    public FetchCustomersViewState.SuccessFetchingCustomers getSubstate() {
        return substate;
    }

    public Customer getChosenCustomer() {
        return chosenCustomer;
    }

    public FetchTablesAction getFetchTablesAction() {
        return fetchTablesAction;
    }

    public ChooseCustomerAction setFetchTablesAction(FetchTablesAction fetchTablesAction) {
        this.fetchTablesAction = fetchTablesAction;
        return this;
    }

}
