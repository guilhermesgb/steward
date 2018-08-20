package com.github.guilhermesgb.steward.mvi.reservation.model;

import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.utils.ViewStateOption;
import com.pacoworks.rxsealedunions2.Union6;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public interface MakeReservationsViewState extends Union6<MakeReservationsViewState.Initial,
        MakeReservationsViewState.CustomerChosen, MakeReservationsViewState.TableChosen,
        MakeReservationsViewState.MakingReservation, MakeReservationsViewState.SuccessMakingReservation,
        MakeReservationsViewState.ErrorMakingReservation> {

    final class Initial extends ViewStateOption implements MakeReservationsViewState {

        private final FetchCustomersViewState substate;

        public Initial(FetchCustomersViewState substate) {
            this.substate = substate;
        }

        public FetchCustomersViewState getSubstate() {
            return substate;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<CustomerChosen> customerChosen,
                              Consumer<TableChosen> tableChosen,
                              Consumer<MakingReservation> makingReservation,
                              Consumer<SuccessMakingReservation> successMakingReservation,
                              Consumer<ErrorMakingReservation> errorMakingReservation) {
            doAccept(initial, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<CustomerChosen, R> customerChosen,
                          Function<TableChosen, R> tableChosen,
                          Function<MakingReservation, R> makingReservation,
                          Function<SuccessMakingReservation, R> successMakingReservation,
                          Function<ErrorMakingReservation, R> errorMakingReservation) {
            return doApply(initial, this);
        }

        @Override
        protected String getOptionName() {
            return "MakeReservationsViewState.Initial";
        }

    }

    final class CustomerChosen extends ViewStateOption implements MakeReservationsViewState {

        private final FetchCustomersViewState.SuccessFetchingCustomers firstSubstate;
        private final Customer chosenCustomer;
        private final FetchTablesViewState secondSubstate;

        public CustomerChosen(FetchCustomersViewState.SuccessFetchingCustomers firstSubstate,
                              Customer chosenCustomer, FetchTablesViewState secondSubstate) {
            this.firstSubstate = firstSubstate;
            this.chosenCustomer = chosenCustomer;
            this.secondSubstate = secondSubstate;
        }

        public FetchCustomersViewState.SuccessFetchingCustomers getFirstSubstate() {
            return firstSubstate;
        }

        public Customer getChosenCustomer() {
            return chosenCustomer;
        }

        public FetchTablesViewState getSecondSubstate() {
            return secondSubstate;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<CustomerChosen> customerChosen,
                              Consumer<TableChosen> tableChosen,
                              Consumer<MakingReservation> makingReservation,
                              Consumer<SuccessMakingReservation> successMakingReservation,
                              Consumer<ErrorMakingReservation> errorMakingReservation) {
            doAccept(customerChosen, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<CustomerChosen, R> customerChosen,
                          Function<TableChosen, R> tableChosen,
                          Function<MakingReservation, R> makingReservation,
                          Function<SuccessMakingReservation, R> successMakingReservation,
                          Function<ErrorMakingReservation, R> errorMakingReservation) {
            return doApply(customerChosen, this);
        }

        @Override
        protected String getOptionName() {
            return "MakeReservationsViewState.CustomerChosen";
        }

    }

    final class TableChosen extends ViewStateOption implements MakeReservationsViewState {

        private final CustomerChosen firstSubstate;
        private final FetchTablesViewState.SuccessFetchingTables secondSubstate;
        private Table chosenTable;

        public TableChosen(CustomerChosen firstSubstate,
                           FetchTablesViewState.SuccessFetchingTables secondSubstate,
                           Table chosenTable) {
            this.firstSubstate = firstSubstate;
            this.secondSubstate = secondSubstate;
            this.chosenTable = chosenTable;
        }

        public CustomerChosen getFirstSubstate() {
            return firstSubstate;
        }

        public FetchTablesViewState.SuccessFetchingTables getSecondSubstate() {
            return secondSubstate;
        }

        public Table getChosenTable() {
            return chosenTable;
        }

        public void setChosenTable(Table chosenTable) {
            this.chosenTable = chosenTable;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<CustomerChosen> customerChosen,
                              Consumer<TableChosen> tableChosen,
                              Consumer<MakingReservation> makingReservation,
                              Consumer<SuccessMakingReservation> successMakingReservation,
                              Consumer<ErrorMakingReservation> errorMakingReservation) {
            doAccept(tableChosen, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<CustomerChosen, R> customerChosen,
                          Function<TableChosen, R> tableChosen,
                          Function<MakingReservation, R> makingReservation,
                          Function<SuccessMakingReservation, R> successMakingReservation,
                          Function<ErrorMakingReservation, R> errorMakingReservation) {
            return doApply(tableChosen, this);
        }

        @Override
        protected String getOptionName() {
            return "MakeReservationsViewState.TableChosen";
        }

    }

    final class MakingReservation extends ViewStateOption implements MakeReservationsViewState {

        private final TableChosen finalSubstate;

        public MakingReservation(TableChosen finalSubstate) {
            this.finalSubstate = finalSubstate;
        }

        public TableChosen getFinalSubstate() {
            return finalSubstate;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<CustomerChosen> customerChosen,
                              Consumer<TableChosen> tableChosen,
                              Consumer<MakingReservation> makingReservation,
                              Consumer<SuccessMakingReservation> successMakingReservation,
                              Consumer<ErrorMakingReservation> errorMakingReservation) {
            doAccept(makingReservation, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<CustomerChosen, R> customerChosen,
                          Function<TableChosen, R> tableChosen,
                          Function<MakingReservation, R> makingReservation,
                          Function<SuccessMakingReservation, R> successMakingReservation,
                          Function<ErrorMakingReservation, R> errorMakingReservation) {
            return doApply(makingReservation, this);
        }

        @Override
        protected String getOptionName() {
            return "MakeReservationsViewState.MakingReservation";
        }

    }

    final class SuccessMakingReservation extends ViewStateOption implements MakeReservationsViewState {

        private final TableChosen finalSubstate;

        public SuccessMakingReservation(TableChosen finalSubstate) {
            this.finalSubstate = finalSubstate;
        }

        public TableChosen getFinalSubstate() {
            return finalSubstate;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<CustomerChosen> customerChosen,
                              Consumer<TableChosen> tableChosen,
                              Consumer<MakingReservation> makingReservation,
                              Consumer<SuccessMakingReservation> successMakingReservation,
                              Consumer<ErrorMakingReservation> errorMakingReservation) {
            doAccept(successMakingReservation, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<CustomerChosen, R> customerChosen,
                          Function<TableChosen, R> tableChosen,
                          Function<MakingReservation, R> makingReservation,
                          Function<SuccessMakingReservation, R> successMakingReservation,
                          Function<ErrorMakingReservation, R> errorMakingReservation) {
            return doApply(successMakingReservation, this);
        }

        @Override
        protected String getOptionName() {
            return "MakeReservationsViewState.SuccessMakingReservation";
        }

    }

    final class ErrorMakingReservation extends ViewStateOption implements MakeReservationsViewState {

        private final TableChosen finalSubstate;
        private final ReservationException exception;
        private Object payload;

        public ErrorMakingReservation(TableChosen finalSubstate, ReservationException exception) {
            this.finalSubstate = finalSubstate;
            this.exception = exception;
        }

        public TableChosen getFinalSubstate() {
            return finalSubstate;
        }

        public ReservationException getException() {
            return exception;
        }

        public Object getPayload() {
            return payload;
        }

        public void setPayload(Object payload) {
            this.payload = payload;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<CustomerChosen> customerChosen,
                              Consumer<TableChosen> tableChosen,
                              Consumer<MakingReservation> makingReservation,
                              Consumer<SuccessMakingReservation> successMakingReservation,
                              Consumer<ErrorMakingReservation> errorMakingReservation) {
            doAccept(errorMakingReservation, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<CustomerChosen, R> customerChosen,
                          Function<TableChosen, R> tableChosen,
                          Function<MakingReservation, R> makingReservation,
                          Function<SuccessMakingReservation, R> successMakingReservation,
                          Function<ErrorMakingReservation, R> errorMakingReservation) {
            return doApply(errorMakingReservation, this);
        }

        @Override
        protected String getOptionName() {
            return "MakeReservationsViewState.ErrorMakingReservation";
        }

    }

}
