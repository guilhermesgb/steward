package com.github.guilhermesgb.steward.mvi.customer.model;

import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.utils.ViewStateOption;
import com.pacoworks.rxsealedunions2.Union4;

import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public interface FetchCustomersViewState extends Union4<FetchCustomersViewState.Initial,
        FetchCustomersViewState.FetchingCustomers, FetchCustomersViewState.SuccessFetchingCustomers,
        FetchCustomersViewState.ErrorFetchingCustomers> {

    final class Initial extends ViewStateOption implements FetchCustomersViewState {

        private List<Customer> cachedCustomers;

        public Initial(List<Customer> cachedCustomers) {
            this.cachedCustomers = cachedCustomers;
        }

        public List<Customer> getCachedCustomers() {
            return cachedCustomers;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingCustomers> fetchingCustomers,
                              Consumer<SuccessFetchingCustomers> successFetchingCustomers,
                              Consumer<ErrorFetchingCustomers> errorFetchingCustomers) {
            doAccept(initial, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingCustomers, R> fetchingCustomers,
                          Function<SuccessFetchingCustomers, R> successFetchingCustomers,
                          Function<ErrorFetchingCustomers, R> errorFetchingCustomers) {
            return doApply(initial, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.Initial";
        }

    }

    final class FetchingCustomers extends ViewStateOption implements FetchCustomersViewState {

        private final FetchCustomersAction action;

        public FetchingCustomers(FetchCustomersAction action) {
            this.action = action;
        }

        public FetchCustomersAction getAction() {
            return action;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingCustomers> fetchingCustomers,
                              Consumer<SuccessFetchingCustomers> successFetchingCustomers,
                              Consumer<ErrorFetchingCustomers> errorFetchingCustomers) {
            doAccept(fetchingCustomers, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingCustomers, R> fetchingCustomers,
                          Function<SuccessFetchingCustomers, R> successFetchingCustomers,
                          Function<ErrorFetchingCustomers, R> errorFetchingCustomers) {
            return doApply(fetchingCustomers, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.FetchingCustomers";
        }

    }

    final class SuccessFetchingCustomers extends ViewStateOption implements FetchCustomersViewState {

        private final FetchCustomersAction action;
        private final List<Customer> customers;

        public SuccessFetchingCustomers(FetchCustomersAction action, List<Customer> customers) {
            this.action = action;
            this.customers = customers;
        }

        public FetchCustomersAction getAction() {
            return action;
        }

        public List<Customer> getCustomers() {
            return customers;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingCustomers> fetchingCustomers,
                              Consumer<SuccessFetchingCustomers> successFetchingCustomers,
                              Consumer<ErrorFetchingCustomers> errorFetchingCustomers) {
            doAccept(successFetchingCustomers, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingCustomers, R> fetchingCustomers,
                          Function<SuccessFetchingCustomers, R> successFetchingCustomers,
                          Function<ErrorFetchingCustomers, R> errorFetchingCustomers) {
            return doApply(successFetchingCustomers, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.SuccessFetchingCustomers";
        }

    }

    final class ErrorFetchingCustomers extends ViewStateOption implements FetchCustomersViewState {

        private final FetchCustomersAction action;
        private final Throwable throwable;
        private List<Customer> cachedCustomers;

        public ErrorFetchingCustomers(FetchCustomersAction action, Throwable throwable) {
            this.action = action;
            this.throwable = throwable;
        }

        public FetchCustomersAction getAction() {
            return action;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public List<Customer> getCachedCustomers() {
            return cachedCustomers;
        }

        public ErrorFetchingCustomers setCachedCustomers(List<Customer> customers) {
            cachedCustomers = customers;
            return this;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingCustomers> fetchingCustomers,
                              Consumer<SuccessFetchingCustomers> successFetchingCustomers,
                              Consumer<ErrorFetchingCustomers> errorFetchingCustomers) {
            doAccept(errorFetchingCustomers, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingCustomers, R> fetchingCustomers,
                          Function<SuccessFetchingCustomers, R> successFetchingCustomers,
                          Function<ErrorFetchingCustomers, R> errorFetchingCustomers) {
            return doApply(errorFetchingCustomers, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.ErrorFetchingCustomers";
        }

    }

}
