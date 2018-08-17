package com.github.guilhermesgb.steward.mvi.table.model;

import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.utils.ViewStateOption;
import com.pacoworks.rxsealedunions2.Union4;

import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public interface FetchTablesViewState extends Union4<FetchTablesViewState.Initial,
        FetchTablesViewState.FetchingTables, FetchTablesViewState.SuccessFetchingTables,
        FetchTablesViewState.ErrorFetchingTables> {

    final class Initial extends ViewStateOption implements FetchTablesViewState {

        private List<Table> cachedTables;

        public Initial(List<Table> cachedTables) {
            this.cachedTables = cachedTables;
        }

        public List<Table> getCachedTables() {
            return cachedTables;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingTables> fetchingTables,
                              Consumer<SuccessFetchingTables> successFetchingTables,
                              Consumer<ErrorFetchingTables> errorFetchingTables) {
            doAccept(initial, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingTables, R> fetchingTables,
                          Function<SuccessFetchingTables, R> successFetchingTables,
                          Function<ErrorFetchingTables, R> errorFetchingTables) {
            return doApply(initial, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchTablesViewState.Initial";
        }

    }

    final class FetchingTables extends ViewStateOption implements FetchTablesViewState {

        private final FetchTablesAction action;

        public FetchingTables(FetchTablesAction action) {
            this.action = action;
        }

        public FetchTablesAction getAction() {
            return action;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingTables> fetchingTables,
                              Consumer<SuccessFetchingTables> successFetchingTables,
                              Consumer<ErrorFetchingTables> errorFetchingTables) {
            doAccept(fetchingTables, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingTables, R> fetchingTables,
                          Function<SuccessFetchingTables, R> successFetchingTables,
                          Function<ErrorFetchingTables, R> errorFetchingTables) {
            return doApply(fetchingTables, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchTablesViewState.FetchingTables";
        }

    }

    final class SuccessFetchingTables extends ViewStateOption implements FetchTablesViewState {

        private final FetchTablesAction action;
        private List<Table> tables;

        public SuccessFetchingTables(FetchTablesAction action, List<Table> tables) {
            this.action = action;
            this.tables = tables;
        }

        public FetchTablesAction getAction() {
            return action;
        }

        public List<Table> getTables() {
            return tables;
        }

        public SuccessFetchingTables setTables(List<Table> tables) {
            this.tables = tables;
            return this;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingTables> fetchingTables,
                              Consumer<SuccessFetchingTables> successFetchingTables,
                              Consumer<ErrorFetchingTables> errorFetchingTables) {
            doAccept(successFetchingTables, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingTables, R> fetchingTables,
                          Function<SuccessFetchingTables, R> successFetchingTables,
                          Function<ErrorFetchingTables, R> errorFetchingTables) {
            return doApply(successFetchingTables, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchTablesViewState.SuccessFetchingTables";
        }

    }

    final class ErrorFetchingTables extends ViewStateOption implements FetchTablesViewState {

        private final FetchTablesAction action;
        private final Throwable throwable;
        private List<Table> cachedTables;

        public ErrorFetchingTables(FetchTablesAction action, Throwable throwable) {
            this.action = action;
            this.throwable = throwable;
        }

        public FetchTablesAction getAction() {
            return action;
        }

        public Throwable getThrowable() {
            return throwable;
        }

        public List<Table> getCachedTables() {
            return cachedTables;
        }

        public ErrorFetchingTables setCachedTables(List<Table> tables) {
            cachedTables = tables;
            return this;
        }

        @Override
        public void continued(Consumer<Initial> initial,
                              Consumer<FetchingTables> fetchingTables,
                              Consumer<SuccessFetchingTables> successFetchingTables,
                              Consumer<ErrorFetchingTables> errorFetchingTables) {
            doAccept(errorFetchingTables, this);
        }

        @Override
        public <R> R join(Function<Initial, R> initial,
                          Function<FetchingTables, R> fetchingTables,
                          Function<SuccessFetchingTables, R> successFetchingTables,
                          Function<ErrorFetchingTables, R> errorFetchingTables) {
            return doApply(errorFetchingTables, this);
        }

        @Override
        protected String getOptionName() {
            return "FetchTablesViewState.ErrorFetchingTables";
        }

    }

}
