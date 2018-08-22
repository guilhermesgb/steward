package com.github.guilhermesgb.steward.mvi.table;

import android.content.Context;

import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.schema.Tables;
import com.github.guilhermesgb.steward.utils.UseCase;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import timber.log.Timber;

public class FetchTablesUseCase extends UseCase {

    public FetchTablesUseCase(String apiBaseUrl, Context context) {
        super(apiBaseUrl, context);
    }

    public Observable<FetchTablesViewState> doFetchTables(final FetchTablesAction action) {
        Observable<FetchTablesViewState> fetchRemoteTables
            = mapListOfTablesToStates(action, getApi().fetchTables()
                .map(new Function<Tables, List<Table>>() {
                    @Override
                    public List<Table> apply(Tables tables) {
                        return tables.getTables();
                    }
                }).toObservable(), false);

        final Observable<FetchTablesViewState> fetchLocalTables = mapListOfTablesToStates
            (action, getDatabase().tableDao().findAll().toObservable(), true);

        return Observable.combineLatest(fetchLocalTables, fetchRemoteTables.cache(),
            new BiFunction<FetchTablesViewState, FetchTablesViewState, FetchTablesViewState>() {
                private FetchTablesViewState mergeLocalStateWithRemoteState(final List<Table> localTables,
                                                                            final FetchTablesViewState remoteState) {
                    return remoteState.join(
                            null,  //Initial state can be safely disregarded here.
                            null, //Same goes for the FetchingTables 'loading' state...
                            new Function<FetchTablesViewState.SuccessFetchingTables, FetchTablesViewState>() {
                                @Override
                                public FetchTablesViewState apply(FetchTablesViewState.SuccessFetchingTables success) {
                                    //In case we have remote tables, we merge the remote state with our local tables,
                                    // prioritizing whatever happened locally since the server is not updated with
                                    // our local work and we don't want to lose it.
                                    //This merge process will be as follows: any new tables will be appended to the set,
                                    // but if we are given less tables than what we currently have locally,
                                    // we keep the local tables nonetheless.
                                    List<Table> remoteTables = success.getTables();
                                    if (remoteTables.size() > localTables.size()) {
                                        List<Table> mergedTables = new LinkedList<>();
                                        for (int i=0; i<remoteTables.size(); i++) {
                                            if (i < localTables.size()) {
                                                mergedTables.add(localTables.get(i));
                                            } else {
                                                mergedTables.add(remoteTables.get(i));
                                            }
                                        }
                                        success = new FetchTablesViewState
                                            .SuccessFetchingTables(action, mergedTables);
                                    } else {
                                        success = success.setTables(localTables);
                                    }
                                    //Persisting merged state in the local database.
                                    try {
                                        getDatabase().beginTransaction();
                                        getDatabase().reservationDao().deleteUnusedTables();
                                        getDatabase().tableDao().insertAll(success.getTables());
                                        getDatabase().setTransactionSuccessful();
                                    } finally {
                                        getDatabase().endTransaction();
                                    }
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
                        .doOnNext(new Consumer<FetchTablesViewState>() {
                            @Override
                            public void accept(FetchTablesViewState state) {
                                Timber.d("PASSING FOLLOWING STATE DOWNSTREAM: %s.", state);
                            }
                        })
                        .switchMap(new Function<FetchTablesViewState, ObservableSource<FetchTablesViewState>>() {
                            @Override
                            public ObservableSource<FetchTablesViewState> apply(FetchTablesViewState state) {
                                if (!(state instanceof FetchTablesViewState.ErrorFetchingTables)) {
                                    return Observable.just(state);
                                }
                                FetchTablesViewState.ErrorFetchingTables error
                                    = (FetchTablesViewState.ErrorFetchingTables) state;
                                FetchTablesViewState.Initial initial
                                    = new FetchTablesViewState.Initial(error.getCachedTables());
                                // Whenever an error occurs, we want to follow up with the initial state again after a 5 seconds.
                                // That's achieved with the combination of zip and take operations below.
                                return Observable.zip(Observable.interval(5, !isBeingTested() ? TimeUnit.SECONDS
                                        : TimeUnit.MILLISECONDS).take(2), Observable.fromArray(error, initial),
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
                                                                     Observable<List<Table>> tables,
                                                                     final boolean localSource) {
        return tables.map(new Function<List<Table>, FetchTablesViewState>() {
            @Override
            public FetchTablesViewState apply(List<Table> tables) {
                return new FetchTablesViewState.SuccessFetchingTables(action, tables);
            }
        }).onErrorReturn(new Function<Throwable, FetchTablesViewState>() {
            @Override
            public FetchTablesViewState apply(Throwable throwable) {
                return localSource ? new FetchTablesViewState
                    .SuccessFetchingTables(action, new LinkedList<Table>())
                    : new FetchTablesViewState.ErrorFetchingTables(action, throwable);
            }
        });
    }

}
