package com.github.guilhermesgb.steward.mvi.customer;

import android.content.Context;

import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
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

public class FetchCustomersUseCase extends UseCase {

    public FetchCustomersUseCase(String apiBaseUrl, Context context) {
        super(apiBaseUrl, context);
    }

    public Observable<FetchCustomersViewState> doFetchCustomers(final FetchCustomersAction action) {
        Observable<FetchCustomersViewState> fetchRemoteCustomers
            = mapListOfCustomersToStates(action, getApi().fetchCustomers().toObservable(), false);

        final Observable<FetchCustomersViewState> fetchLocalCustomers = mapListOfCustomersToStates
            (action, getDatabase().customerDao().findAll().toObservable(), true);

        return Observable.combineLatest(fetchLocalCustomers, fetchRemoteCustomers.cache(),
            new BiFunction<FetchCustomersViewState, FetchCustomersViewState, FetchCustomersViewState>() {
                private FetchCustomersViewState mergeLocalStateWithRemoteState(final List<Customer> localCustomers,
                                                                               FetchCustomersViewState remoteState) {
                    return remoteState.join(
                            null,  //Initial state can be safely disregarded here.
                            null, //Same goes for the FetchingCustomers 'loading' state...
                            new Function<FetchCustomersViewState.SuccessFetchingCustomers, FetchCustomersViewState>() {
                                @Override
                                public FetchCustomersViewState apply(FetchCustomersViewState.SuccessFetchingCustomers success) {
                                    //In case we have remote customers, we discard local state in favor of remote state.
                                    //Persisting merged state in the local database.
                                    try {
                                        getDatabase().beginTransaction();
                                        getDatabase().reservationDao().deleteUnusedCustomers();
                                        getDatabase().customerDao().insertAll(success.getCustomers());
                                        getDatabase().setTransactionSuccessful();
                                    } finally {
                                        getDatabase().endTransaction();
                                    }
                                    return success;
                                }
                            },
                            new Function<FetchCustomersViewState.ErrorFetchingCustomers, FetchCustomersViewState>() {
                                @Override
                                public FetchCustomersViewState apply(FetchCustomersViewState.ErrorFetchingCustomers error) {
                                    //In case we can't fetch remote customers, we merge the remote error state with our local customers.
                                    return new FetchCustomersViewState.ErrorFetchingCustomers
                                        (action, error.getThrowable()).setCachedCustomers(localCustomers);
                                }
                            }
                        );
                }

                @Override
                public FetchCustomersViewState apply(FetchCustomersViewState localState,
                                                     final FetchCustomersViewState remoteState) {
                    return localState.join(
                            null,  //Initial state can be safely disregarded here.
                            null, //Same goes for the FetchingCustomers 'loading' state...
                            new Function<FetchCustomersViewState.SuccessFetchingCustomers, FetchCustomersViewState>() {
                                @Override
                                public FetchCustomersViewState apply(FetchCustomersViewState.SuccessFetchingCustomers success) {
                                    return mergeLocalStateWithRemoteState(success.getCustomers(), remoteState);
                                }
                            },
                            new Function<FetchCustomersViewState.ErrorFetchingCustomers, FetchCustomersViewState>() {
                                @Override
                                public FetchCustomersViewState apply(FetchCustomersViewState.ErrorFetchingCustomers error) {
                                    //In case of errors fetching local customers we rely entirely on the remote state.
                                    return remoteState;
                                }
                            }
                        );
                }
            })
            .publish(new Function<Observable<FetchCustomersViewState>, ObservableSource<FetchCustomersViewState>>() {
                @Override
                public ObservableSource<FetchCustomersViewState> apply(Observable<FetchCustomersViewState> fetchRemoteCustomers) {
                    List<Observable<FetchCustomersViewState>> operations = new LinkedList<>();
                    //The original stream of local customers gets priority below.
                    operations.add(fetchLocalCustomers);
                    //The composite stream of remote customers merged with local customers
                    // follows behind. It will always contain merges of latest remote states and local states
                    // which implicitly means it will never produce at a faster rate than the original stream
                    // of local customers above, by definition.
                    //So the resulting stream below will return local customer states as soon as possible
                    // and then rely remote customer states merged with local customer states to simulate
                    // a caching effect in case of network errors preventing retrieval of remote state.
                    operations.add(fetchRemoteCustomers);
                    return Observable.concatEager(operations)
                        .startWith(new FetchCustomersViewState.FetchingCustomers(action))
                        .doOnNext(new Consumer<FetchCustomersViewState>() {
                            @Override
                            public void accept(FetchCustomersViewState state) {
                                Timber.d("PASSING FOLLOWING STATE DOWNSTREAM: %s.", state);
                            }
                        })
                        .switchMap(new Function<FetchCustomersViewState, ObservableSource<FetchCustomersViewState>>() {
                            @Override
                            public ObservableSource<FetchCustomersViewState> apply(FetchCustomersViewState state) {
                                if (!(state instanceof FetchCustomersViewState.ErrorFetchingCustomers)) {
                                    return Observable.just(state);
                                }
                                FetchCustomersViewState.ErrorFetchingCustomers error
                                    = (FetchCustomersViewState.ErrorFetchingCustomers) state;
                                FetchCustomersViewState.Initial initial
                                    = new FetchCustomersViewState.Initial(error.getCachedCustomers());
                                // Whenever an error occurs, we want to follow up with the initial state again after a 5 seconds.
                                // That's achieved with the combination of zip and take operations below.
                                return Observable.zip(Observable.interval(5, !isBeingTested() ? TimeUnit.SECONDS
                                        : TimeUnit.MILLISECONDS).take(2), Observable.fromArray(error, initial),
                                    new BiFunction<Long, FetchCustomersViewState, FetchCustomersViewState>() {
                                        @Override
                                        public FetchCustomersViewState apply(Long ignore, FetchCustomersViewState state) {
                                            return state;
                                        }
                                    });
                            }
                        });
                }
            });
    }

    private Observable<FetchCustomersViewState> mapListOfCustomersToStates(final FetchCustomersAction action,
                                                                           Observable<List<Customer>> customers,
                                                                           final boolean localSource) {
        return customers.map(new Function<List<Customer>, FetchCustomersViewState>() {
            @Override
            public FetchCustomersViewState apply(List<Customer> customers) {
                return new FetchCustomersViewState.SuccessFetchingCustomers(action, customers);
            }
        }).onErrorReturn(new Function<Throwable, FetchCustomersViewState>() {
            @Override
            public FetchCustomersViewState apply(Throwable throwable) {
                return localSource ? new FetchCustomersViewState
                    .SuccessFetchingCustomers(action, new LinkedList<Customer>())
                    : new FetchCustomersViewState.ErrorFetchingCustomers(action, throwable);
            }
        });
    }

}
