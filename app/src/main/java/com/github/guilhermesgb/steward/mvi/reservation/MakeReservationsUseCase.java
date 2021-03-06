package com.github.guilhermesgb.steward.mvi.reservation;

import android.annotation.SuppressLint;
import android.content.Context;

import com.github.guilhermesgb.steward.mvi.customer.FetchCustomersUseCase;
import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseCustomerAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseTableAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ConfirmReservationAction;
import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;
import com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException;
import com.github.guilhermesgb.steward.mvi.reservation.schema.Reservation;
import com.github.guilhermesgb.steward.mvi.table.FetchTablesUseCase;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.utils.UseCase;

import org.joda.time.DateTime;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.CUSTOMER_BUSY;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.CUSTOMER_NOT_FOUND;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.DATABASE_FAILURE;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.RESERVATION_IN_PLACE;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.TABLE_NOT_FOUND;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.TABLE_UNAVAILABLE;
import static com.github.guilhermesgb.steward.utils.DateUtils.formatDate;

public class MakeReservationsUseCase extends UseCase {

    private FetchCustomersUseCase fetchCustomersUseCase;
    private FetchTablesUseCase fetchTablesUseCase;

    public MakeReservationsUseCase(String apiBaseUrl, Context context) {
        super(apiBaseUrl, context);
        this.fetchCustomersUseCase = new FetchCustomersUseCase(apiBaseUrl, context);
        this.fetchTablesUseCase = new FetchTablesUseCase(apiBaseUrl, context);
    }

    @Override
    public void setBeingTested() {
        super.setBeingTested();
        fetchCustomersUseCase.setBeingTested();
        fetchTablesUseCase.setBeingTested();
    }

    public FetchCustomersUseCase getFetchCustomersUseCase() {
        return fetchCustomersUseCase;
    }

    public void setFetchCustomersUseCase(FetchCustomersUseCase fetchCustomersUseCase) {
        this.fetchCustomersUseCase = fetchCustomersUseCase;
    }

    public FetchTablesUseCase getFetchTablesUseCase() {
        return fetchTablesUseCase;
    }

    public void setFetchTablesUseCase(FetchTablesUseCase fetchTablesUseCase) {
        this.fetchTablesUseCase = fetchTablesUseCase;
    }

    public Observable<MakeReservationsViewState> fetchCustomers(final FetchCustomersAction action) {
        return fetchCustomersUseCase.doFetchCustomers(action)
            .map(new Function<FetchCustomersViewState, MakeReservationsViewState>() {
                @Override
                public MakeReservationsViewState apply(FetchCustomersViewState firstSubstate) {
                    return new MakeReservationsViewState.Initial(firstSubstate);
                }
            })
            .doOnNext(new Consumer<MakeReservationsViewState>() {
                @Override
                public void accept(MakeReservationsViewState state) {
                    Timber.d("PASSING FOLLOWING STATE DOWNSTREAM: %s.", state);
                }
            });
    }

    public Observable<MakeReservationsViewState> chooseCustomer(final ChooseCustomerAction action) {
        return refreshTables(action);
    }

    private Observable<MakeReservationsViewState> refreshTables(final ChooseCustomerAction action) {
        return fetchTablesUseCase.doFetchTables(action.getFetchTablesAction())
            .map(new Function<FetchTablesViewState, MakeReservationsViewState>() {
                @Override
                public MakeReservationsViewState apply(FetchTablesViewState secondSubstate) {
                    return new MakeReservationsViewState.CustomerChosen
                        (action.getSubstate(), action.getChosenCustomer(), secondSubstate);
                }
            })
            .doOnNext(new Consumer<MakeReservationsViewState>() {
                @Override
                public void accept(MakeReservationsViewState state) {
                    Timber.d("PASSING FOLLOWING STATE DOWNSTREAM: %s.", state);
                }
            });
    }

    public Observable<MakeReservationsViewState> chooseTable(final ChooseTableAction action) {
        return Observable.<MakeReservationsViewState>just(new MakeReservationsViewState.TableChosen
            (action.getFirstSubstate(), action.getSecondSubstate(), action.getChosenTable()))
            .doOnNext(new Consumer<MakeReservationsViewState>() {
                @Override
                public void accept(MakeReservationsViewState state) {
                    Timber.d("PASSING FOLLOWING STATE DOWNSTREAM: %s.", state);
                }
            });
    }

    @SuppressLint("CheckResult")
    public Observable<MakeReservationsViewState> confirmReservation(final ConfirmReservationAction action) {
        final MakeReservationsViewState.TableChosen finalSubstate = action.getFinalSubstate();
        final Table chosenTable = finalSubstate.getChosenTable();
        final Customer chosenCustomer = finalSubstate.getFirstSubstate().getChosenCustomer();
        //I need to do the following things here:
        //First, I need to check that the chosen customer still exists...
        try {
            getDatabase().customerDao().findById(chosenCustomer.getId())
                .subscribeOn(Schedulers.io()).blockingGet();
        } catch (Throwable throwable) {
            ReservationException reservationException
                = new ReservationException(CUSTOMER_NOT_FOUND, throwable);
            return Observable.<MakeReservationsViewState>just
                (new MakeReservationsViewState.ErrorMakingReservation
                    (finalSubstate, reservationException))
                    .startWith(new MakeReservationsViewState
                        .MakingReservation(finalSubstate));
        }
        // ...and then check the chosen customer does not have an active reservation.
        return getDatabase().reservationDao().findTablesForGivenCustomer(chosenCustomer.getId()).toObservable()
            .switchMap(new Function<List<Table>, ObservableSource<MakeReservationsViewState>>() {
                @Override
                public ObservableSource<MakeReservationsViewState> apply(List<Table> tables) {
                    if (tables.isEmpty()) {
                        //Second, I need to check that the chosen table is still available.
                        return getDatabase().tableDao().findByNumber(chosenTable.getNumber()).toObservable()
                            .switchMap(new Function<Table, ObservableSource<MakeReservationsViewState>>() {
                                @Override
                                public ObservableSource<MakeReservationsViewState> apply(Table table) {
                                    if (table.isAvailable()) {
                                        //Finally, I need to create a new reservation for the given customer at given table, set to expire
                                        //  in 10 minutes from now (simulating the time needed for the customer to use the table for their needs).
                                        table.setAvailable(false);
                                        try {
                                            getDatabase().beginTransaction();
                                            getDatabase().tableDao().insert(table);
                                            getDatabase().reservationDao().deleteAllForCustomer(chosenCustomer.getId());
                                            getDatabase().reservationDao().insert(new Reservation(chosenCustomer.getId(),
                                                chosenTable.getNumber(), formatDate(DateTime.now().plusMinutes(10))));
                                            getDatabase().setTransactionSuccessful();
                                            return Observable.<MakeReservationsViewState>just(new MakeReservationsViewState
                                                .SuccessMakingReservation(new MakeReservationsViewState
                                                    .TableChosen(finalSubstate.getFirstSubstate(),
                                                    finalSubstate.getSecondSubstate(), table)));
                                        } catch (Throwable throwable) {
                                            ReservationException reservationException
                                                = new ReservationException(DATABASE_FAILURE, throwable);
                                            return Observable.<MakeReservationsViewState>just
                                                (new MakeReservationsViewState.ErrorMakingReservation
                                                    (finalSubstate, reservationException));
                                        } finally {
                                            getDatabase().endTransaction();
                                        }
                                    } else {
                                        ReservationException reservationException
                                            = new ReservationException(TABLE_UNAVAILABLE);
                                        return Observable.<MakeReservationsViewState>just
                                            (new MakeReservationsViewState.ErrorMakingReservation
                                                (new MakeReservationsViewState.TableChosen
                                                    (finalSubstate.getFirstSubstate(),
                                                        finalSubstate.getSecondSubstate(),
                                                            table), reservationException));
                                    }
                                }
                            });
                    } else {
                        //The customer already has a reservation - check whether the customer isn't already in the desired table,
                        // in which case I shall issue a friendly reminder that the customer is already placed at given table.
                        ReservationException reservationException = null;
                        MakeReservationsViewState.ErrorMakingReservation errorState = null;
                        for (Table table : tables) {
                            if (table.getNumber() == chosenTable.getNumber()) {
                                reservationException = new ReservationException(RESERVATION_IN_PLACE);
                                errorState = new MakeReservationsViewState.ErrorMakingReservation
                                    (new MakeReservationsViewState.TableChosen(finalSubstate.getFirstSubstate(),
                                        finalSubstate.getSecondSubstate(), table), reservationException);
                                break;
                            }
                        }
                        if (reservationException == null) {
                            reservationException = new ReservationException(CUSTOMER_BUSY);
                            errorState = new MakeReservationsViewState.ErrorMakingReservation
                                (finalSubstate, reservationException);
                            errorState.setPayload(tables);
                        }
                        return Observable.<MakeReservationsViewState>just(errorState);
                    }
                }
            })
            .onErrorReturn(new Function<Throwable, MakeReservationsViewState>() {
                @Override
                public MakeReservationsViewState apply(Throwable throwable) {
                    ReservationException reservationException
                        = new ReservationException(TABLE_NOT_FOUND, throwable);
                    return new MakeReservationsViewState.ErrorMakingReservation
                        (finalSubstate, reservationException);
                }
            })
            .startWith(new MakeReservationsViewState.MakingReservation(finalSubstate))
            .doOnNext(new Consumer<MakeReservationsViewState>() {
                @Override
                public void accept(MakeReservationsViewState state) {
                    Timber.d("PASSING FOLLOWING STATE DOWNSTREAM: %s.", state);
                }
            });
    }

}
