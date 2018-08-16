package com.github.guilhermesgb.steward.mvi.customer.model;

import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.utils.ViewStateOption;
import com.pacoworks.rxsealedunions2.Union7;

import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public interface FetchCustomersViewState extends Union7<FetchCustomersViewState.Initial,
        FetchCustomersViewState.FetchingCustomers, FetchCustomersViewState.SuccessFetchingCustomers,
        FetchCustomersViewState.ErrorFetchingCustomers, FetchCustomersViewState.SearchingCustomers,
        FetchCustomersViewState.SuccessSearchingCustomers, FetchCustomersViewState.ErrorSearchingCustomers> {

    final class Initial extends ViewStateOption implements FetchCustomersViewState {

        @Override
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function,
                          Function<FetchingCustomers, R> function1,
                          Function<SuccessFetchingCustomers, R> function2,
                          Function<ErrorFetchingCustomers, R> function3,
                          Function<SearchingCustomers, R> function4,
                          Function<SuccessSearchingCustomers, R> function5,
                          Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function, this);
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
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer1, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function, Function<FetchingCustomers, R> function1, Function<SuccessFetchingCustomers, R> function2, Function<ErrorFetchingCustomers, R> function3, Function<SearchingCustomers, R> function4, Function<SuccessSearchingCustomers, R> function5, Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function1, this);
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
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer2, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function,
                          Function<FetchingCustomers, R> function1,
                          Function<SuccessFetchingCustomers, R> function2,
                          Function<ErrorFetchingCustomers, R> function3,
                          Function<SearchingCustomers, R> function4,
                          Function<SuccessSearchingCustomers, R> function5,
                          Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function2, this);
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
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer3, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function,
                          Function<FetchingCustomers, R> function1,
                          Function<SuccessFetchingCustomers, R> function2,
                          Function<ErrorFetchingCustomers, R> function3,
                          Function<SearchingCustomers, R> function4,
                          Function<SuccessSearchingCustomers, R> function5,
                          Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function3, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.ErrorFetchingCustomers";
        }

    }

    final class SearchingCustomers extends ViewStateOption implements FetchCustomersViewState {

        @Override
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer4, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function,
                          Function<FetchingCustomers, R> function1,
                          Function<SuccessFetchingCustomers, R> function2,
                          Function<ErrorFetchingCustomers, R> function3,
                          Function<SearchingCustomers, R> function4,
                          Function<SuccessSearchingCustomers, R> function5,
                          Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function4, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.SearchingCustomers";
        }

    }

    final class SuccessSearchingCustomers extends ViewStateOption implements FetchCustomersViewState {

        @Override
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer5, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function,
                          Function<FetchingCustomers, R> function1,
                          Function<SuccessFetchingCustomers, R> function2,
                          Function<ErrorFetchingCustomers, R> function3,
                          Function<SearchingCustomers, R> function4,
                          Function<SuccessSearchingCustomers, R> function5,
                          Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function5, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.SuccessSearchingCustomers";
        }

    }

    final class ErrorSearchingCustomers extends ViewStateOption implements FetchCustomersViewState {

        @Override
        public void continued(Consumer<Initial> consumer,
                              Consumer<FetchingCustomers> consumer1,
                              Consumer<SuccessFetchingCustomers> consumer2,
                              Consumer<ErrorFetchingCustomers> consumer3,
                              Consumer<SearchingCustomers> consumer4,
                              Consumer<SuccessSearchingCustomers> consumer5,
                              Consumer<ErrorSearchingCustomers> consumer6) {
            doAccept(consumer6, this);
        }

        @Override
        public <R> R join(Function<Initial, R> function,
                          Function<FetchingCustomers, R> function1,
                          Function<SuccessFetchingCustomers, R> function2,
                          Function<ErrorFetchingCustomers, R> function3,
                          Function<SearchingCustomers, R> function4,
                          Function<SuccessSearchingCustomers, R> function5,
                          Function<ErrorSearchingCustomers, R> function6) {
            return doApply(function6, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchCustomersViewState.ErrorSearchingCustomers";
        }

    }

}
