package com.github.guilhermesgb.steward.mvi.reservation.presenter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.reservation.MakeReservationsUseCase;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseCustomerAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseTableAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ConfirmReservationAction;
import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;
import com.github.guilhermesgb.steward.mvi.reservation.view.MakeReservationsView;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.utils.OnReadyPresenter;

import java.util.LinkedList;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.github.guilhermesgb.steward.network.ApiResource.WILL_USE_REAL_API;

public class MakeReservationsPresenter extends OnReadyPresenter<MakeReservationsView, MakeReservationsViewState> {

    private final MakeReservationsUseCase makeReservationsUseCase;

    public MakeReservationsPresenter(Context context) {
        this.makeReservationsUseCase = new MakeReservationsUseCase(WILL_USE_REAL_API, context);
    }

    @Override
    protected void bindIntents() {
        //Here, with aid of the Mosby library, we close the loop, building our MVI cycle,
        // and thus achieving unidirectional data flow with the use case business logic
        // as the single source of truth, everything else reflecting its state.

        final Observable<MakeReservationsViewState> fetchCustomers = intent(
            new ViewIntentBinder<MakeReservationsView, FetchCustomersAction>() {
                @NonNull
                @Override
                public Observable<FetchCustomersAction> bind(@NonNull MakeReservationsView view) {
                    return view.fetchCustomersIntent();
                }
            }
        ).switchMap(new Function<FetchCustomersAction, ObservableSource<MakeReservationsViewState>>() {
            @Override
            public ObservableSource<MakeReservationsViewState> apply(FetchCustomersAction action) {
                return makeReservationsUseCase.fetchCustomers(action)
                    .subscribeOn(Schedulers.io());
            }
        })
        .observeOn(AndroidSchedulers.mainThread());

        final Observable<ChooseCustomerAction> chooseCustomer = intent(
            new ViewIntentBinder<MakeReservationsView, ChooseCustomerAction>() {
                @NonNull
                @Override
                public Observable<ChooseCustomerAction> bind(@NonNull MakeReservationsView view) {
                    return view.chooseCustomerIntent();
                }
            }
        );

        final Observable<FetchTablesAction> refreshTables = intent(
            new ViewIntentBinder<MakeReservationsView, FetchTablesAction>() {
                @NonNull
                @Override
                public Observable<FetchTablesAction> bind(@NonNull MakeReservationsView view) {
                    return view.fetchTablesIntent();
                }
            }
        );

        final Observable<MakeReservationsViewState> chooseCustomerAndRefreshTables
            = Observable.combineLatest(chooseCustomer, refreshTables,
                new BiFunction<ChooseCustomerAction, FetchTablesAction, ChooseCustomerAction>() {
                    @Override
                    public ChooseCustomerAction apply(ChooseCustomerAction firstAction, FetchTablesAction secondAction) {
                        return firstAction.setFetchTablesAction(secondAction);
                    }
                })
            .switchMap(new Function<ChooseCustomerAction, ObservableSource<MakeReservationsViewState>>() {
                @Override
                public ObservableSource<MakeReservationsViewState> apply(ChooseCustomerAction action) {
                    return makeReservationsUseCase.chooseCustomer(action)
                        .subscribeOn(Schedulers.io());
                }
            })
            .observeOn(AndroidSchedulers.mainThread());

        final Observable<MakeReservationsViewState> chooseTable = intent(
            new ViewIntentBinder<MakeReservationsView, ChooseTableAction>() {
                @NonNull
                @Override
                public Observable<ChooseTableAction> bind(@NonNull MakeReservationsView view) {
                    return view.chooseTableIntent();
                }
            }
        ).switchMap(new Function<ChooseTableAction, ObservableSource<MakeReservationsViewState>>() {
            @Override
            public ObservableSource<MakeReservationsViewState> apply(ChooseTableAction action) {
                return makeReservationsUseCase.chooseTable(action)
                    .subscribeOn(Schedulers.io());
            }
        })
        .observeOn(AndroidSchedulers.mainThread());

        final Observable<MakeReservationsViewState> confirmReservation = intent(
            new ViewIntentBinder<MakeReservationsView, ConfirmReservationAction>() {
                @NonNull
                @Override
                public Observable<ConfirmReservationAction> bind(@NonNull MakeReservationsView view) {
                    return view.confirmReservationIntent();
                }
            }
        ).switchMap(new Function<ConfirmReservationAction, ObservableSource<MakeReservationsViewState>>() {
            @Override
            public ObservableSource<MakeReservationsViewState> apply(ConfirmReservationAction action) {
                return makeReservationsUseCase.confirmReservation(action)
                    .subscribeOn(Schedulers.io());
            }
        })
        .observeOn(AndroidSchedulers.mainThread());

        MakeReservationsViewState.Initial initialState = new MakeReservationsViewState.Initial
            (new FetchCustomersViewState.Initial(new LinkedList<Customer>()));
        subscribeViewState(Observable.merge(
                fetchCustomers,
                chooseCustomerAndRefreshTables,
                chooseTable,
                confirmReservation
            ).startWith(initialState),
            new ViewStateConsumer<MakeReservationsView, MakeReservationsViewState>() {
                @Override
                public void accept(final @NonNull MakeReservationsView view, @NonNull MakeReservationsViewState state) {
                    state.continued(new Consumer<MakeReservationsViewState.Initial>() {
                        @Override
                        public void accept(MakeReservationsViewState.Initial state) {
                            view.render(state);
                        }
                    }, new Consumer<MakeReservationsViewState.CustomerChosen>() {
                        @Override
                        public void accept(MakeReservationsViewState.CustomerChosen state) {
                            view.render(state);
                        }
                    }, new Consumer<MakeReservationsViewState.TableChosen>() {
                        @Override
                        public void accept(MakeReservationsViewState.TableChosen state) {
                            view.render(state);
                        }
                    }, new Consumer<MakeReservationsViewState.MakingReservation>() {
                        @Override
                        public void accept(MakeReservationsViewState.MakingReservation state) {
                            view.render(state);
                        }
                    }, new Consumer<MakeReservationsViewState.SuccessMakingReservation>() {
                        @Override
                        public void accept(MakeReservationsViewState.SuccessMakingReservation state) {
                            view.render(state);
                        }
                    }, new Consumer<MakeReservationsViewState.ErrorMakingReservation>() {
                        @Override
                        public void accept(MakeReservationsViewState.ErrorMakingReservation state) {
                            view.render(state);
                        }
                    });
                }
            });
    }

}
