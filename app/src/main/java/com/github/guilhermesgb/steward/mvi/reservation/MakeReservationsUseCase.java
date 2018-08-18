package com.github.guilhermesgb.steward.mvi.reservation;

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
import io.reactivex.functions.Function;

import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.CUSTOMER_BUSY;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.RESERVATION_IN_PLACE;
import static com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException.Code.TABLE_UNAVAILABLE;
import static com.github.guilhermesgb.steward.utils.DateUtils.formatDate;

public class MakeReservationsUseCase extends UseCase {

    private final FetchCustomersUseCase fetchCustomersUseCase;
    private final FetchTablesUseCase fetchTablesUseCase;

    public MakeReservationsUseCase(String apiBaseUrl, Context context) {
        super(apiBaseUrl, context);
        this.fetchCustomersUseCase = new FetchCustomersUseCase(apiBaseUrl, context);
        this.fetchTablesUseCase = new FetchTablesUseCase(apiBaseUrl, context);
    }

    public Observable<MakeReservationsViewState> fetchCustomers(final FetchCustomersAction action) {
        return fetchCustomersUseCase.doFetchCustomers(action)
            .map(new Function<FetchCustomersViewState, MakeReservationsViewState>() {
                @Override
                public MakeReservationsViewState apply(FetchCustomersViewState firstSubstate) {
                    return new MakeReservationsViewState.Initial(firstSubstate);
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
            });
    }

    public Observable<MakeReservationsViewState> chooseTable(final ChooseTableAction action) {
        return Observable.<MakeReservationsViewState>just(new MakeReservationsViewState.TableChosen
            (action.getFirstSubstate(), action.getSecondSubstate(), action.getChosenTable()));
    }

    public Observable<MakeReservationsViewState> confirmReservation(final ConfirmReservationAction action) {
        final Table chosenTable = action.getFinalSubstate().getChosenTable();
        final Customer chosenCustomer = action.getFinalSubstate().getFirstSubstate().getChosenCustomer();
        //I need to do the following things here:
        //First, I need to check that the chosen customer still exists and is not with some active reservation.
        return getDatabase().reservationDao().findTablesForGivenCustomer(chosenCustomer.getId()).toObservable()
            .switchMap(new Function<List<Table>, ObservableSource<MakeReservationsViewState>>() {
                @Override
                public ObservableSource<MakeReservationsViewState> apply(List<Table> tables) {
                    if (tables.isEmpty()) {
                        //Second, I need to check that the chosen table is still available.
                        return getDatabase().tableDao().findTableByNumber(chosenTable.getNumber()).toObservable()
                            .switchMap(new Function<Table, ObservableSource<MakeReservationsViewState>>() {
                                @Override
                                public ObservableSource<MakeReservationsViewState> apply(Table table) {
                                    if (table.isAvailable()) {
                                        //Finally, I need to create a new reservation for the given customer at given table, set to expire
                                        //  in 10 minutes from now (simulating the time needed for the customer to use the table for their needs).
                                        table.setAvailable(false);
                                        getDatabase().tableDao().insert(table);
                                        getDatabase().reservationDao().deleteAllForCustomer(chosenCustomer.getId());
                                        getDatabase().reservationDao().insert(new Reservation(chosenCustomer.getId(),
                                            chosenTable.getNumber(), formatDate(DateTime.now().plusMinutes(10))));
                                        return Observable.<MakeReservationsViewState>just(new MakeReservationsViewState
                                            .SuccessMakingReservation(action.getFinalSubstate()));
                                    } else {
                                        ReservationException reservationException
                                            = new ReservationException(TABLE_UNAVAILABLE);
                                        return Observable.<MakeReservationsViewState>just
                                            (new MakeReservationsViewState.ErrorMakingReservation
                                                (action.getFinalSubstate(), reservationException));
                                    }
                                }
                            });
                    } else {
                        //The customer already has a reservation - check whether the customer isn't already in the desired table,
                        // in which case I shall issue a friendly reminder that the customer is already placed at given table.
                        ReservationException reservationException = null;
                        for (Table table : tables) {
                            if (table.getNumber() == chosenTable.getNumber()) {
                                reservationException = new ReservationException(RESERVATION_IN_PLACE);
                                break;
                            }
                        }
                        if (reservationException == null) {
                            reservationException = new ReservationException(CUSTOMER_BUSY);
                        }
                        return Observable.<MakeReservationsViewState>just(new MakeReservationsViewState
                            .ErrorMakingReservation(action.getFinalSubstate(), reservationException));
                    }
                }
            });
    }

}
