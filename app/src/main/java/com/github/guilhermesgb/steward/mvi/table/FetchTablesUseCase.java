package com.github.guilhermesgb.steward.mvi.table;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.schema.Tables;
import com.github.guilhermesgb.steward.network.ApiResource;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Function;

import static com.github.guilhermesgb.steward.network.ApiResource.WILL_USE_REAL_API;

public class FetchTablesUseCase {

    private final Context context;

    public FetchTablesUseCase(Context context) {
        this.context = context;
    }

    public Observable<FetchTablesViewState> doFetchTables(final FetchTablesAction action) {
        Observable<FetchTablesViewState> fetchRemoteTables = mapListOfTablesToStates
            (action, ApiResource.getInstance(WILL_USE_REAL_API).fetchTables()
                .map(new Function<Tables, List<Table>>() {
                    @Override
                    public List<Table> apply(Tables tables) {
                        return tables.getTables();
                    }
                }).toObservable());

        final Observable<FetchTablesViewState> fetchLocalTables = mapListOfTablesToStates
            (action, DatabaseResource.getInstance(context).tableDao().findAll().toObservable());

        return Observable.combineLatest(fetchLocalTables, fetchRemoteTables.cache(),
            new BiFunction<FetchTablesViewState, FetchTablesViewState, FetchTablesViewState>() {
                private FetchTablesViewState mergeLocalStateWithRemoteState(final List<Table> localTables,
                                                                            FetchTablesViewState remoteState) {
                    return remoteState.join(
                            null,  //Initial state can be safely disregarded here.
                            null, //Same goes for the FetchingTables 'loading' state...
                            new Function<FetchTablesViewState.SuccessFetchingTables, FetchTablesViewState>() {
                                @Override
                                public FetchTablesViewState apply(FetchTablesViewState.SuccessFetchingTables success) {
                                    //In case we have remote tables, we discard local state in favor of remote state.
                                    return success;
                                }
                            },
                            new Function<FetchTablesViewState.ErrorFetchingTables, FetchTablesViewState>() {
                                @Override
                                public FetchTablesViewState apply(FetchTablesViewState.ErrorFetchingTables error) {
                                    //In case we can't fetch remote tables, we merge the remote error state with our local tables.
                                    return new FetchTablesViewState.ErrorFetchingTables
                                        (action, error.getThrowable()).setCachedTables(localTables);
                                }
                            }
                        );
                }

                @Override
                public FetchTablesViewState apply(FetchTablesViewState localState,
                                                  final FetchTablesViewState remoteState) {
                    return localState.join(
                            null,  //Initial state can be safely disregarded here.
                            null, //Same goes for the FetchingTables 'loading' state...
                            new Function<FetchTablesViewState.SuccessFetchingTables, FetchTablesViewState>() {
                                @Override
                                public FetchTablesViewState apply(FetchTablesViewState.SuccessFetchingTables success) {
                                    return mergeLocalStateWithRemoteState(success.getTables(), remoteState);
                                }
                            },
                            new Function<FetchTablesViewState.ErrorFetchingTables, FetchTablesViewState>() {
                                @Override
                                public FetchTablesViewState apply(FetchTablesViewState.ErrorFetchingTables error) {
                                    //In case of errors fetching local tables we rely entirely on the remote state.
                                    return remoteState;
                                }
                            }
                        );
                }
            })
            .publish(new Function<Observable<FetchTablesViewState>, ObservableSource<FetchTablesViewState>>() {
                @Override
                public ObservableSource<FetchTablesViewState> apply(Observable<FetchTablesViewState> fetchRemoteTables) {
                    List<Observable<FetchTablesViewState>> operations = new LinkedList<>();
                    //The original stream of local tables gets priority below.
                    operations.add(fetchLocalTables);
                    //The composite stream of remote tables merged with local tables
                    // follows behind. It will always contain merges of latest remote states and local states
                    // which implicitly means it will never produce at a faster rate than the original stream
                    // of local tables above, by definition.
                    //So the resulting stream below will return local table states as soon as possible
                    // and then rely remote table states merged with local table states to simulate
                    // a caching effect in case of network errors preventing retrieval of remote state.
                    operations.add(fetchRemoteTables);
                    return Observable.concatEager(operations)
                        .startWith(new FetchTablesViewState.FetchingTables(action))
                        .switchMap(new Function<FetchTablesViewState, ObservableSource<FetchTablesViewState>>() {
                            @Override
                            public ObservableSource<FetchTablesViewState> apply(FetchTablesViewState state) {
                                if (!(state instanceof FetchTablesViewState.ErrorFetchingTables)) {
                                    return Observable.just(state);
                                }
                                // Whenever an error occurs, we want to follow up with the initial state again after a 5 seconds.
                                // That's achieved with the combination of zip and take operations below.
                                return Observable.zip(Observable.interval(5, TimeUnit.SECONDS).take(2),
                                    Observable.fromArray(state, new FetchTablesViewState.Initial()),
                                    new BiFunction<Long, FetchTablesViewState, FetchTablesViewState>() {
                                        @Override
                                        public FetchTablesViewState apply(Long ignore, FetchTablesViewState state) {
                                            return state;
                                        }
                                    });
                            }
                        });
                }
            });
    }

    private Observable<FetchTablesViewState> mapListOfTablesToStates(final FetchTablesAction action,
                                                                     Observable<List<Table>> tables) {
        return tables.map(new Function<List<Table>, FetchTablesViewState>() {
            @Override
            public FetchTablesViewState apply(List<Table> tables) {
                return new FetchTablesViewState.SuccessFetchingTables(action, tables);
            }
        }).onErrorReturn(new Function<Throwable, FetchTablesViewState>() {
            @Override
            public FetchTablesViewState apply(Throwable throwable) {
                return new FetchTablesViewState.ErrorFetchingTables(action, throwable);
            }
        });
    }

}
