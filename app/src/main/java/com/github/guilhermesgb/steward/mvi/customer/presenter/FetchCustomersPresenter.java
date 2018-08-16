package com.github.guilhermesgb.steward.mvi.customer.presenter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.guilhermesgb.steward.mvi.customer.FetchCustomersUseCase;
import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.view.FetchCustomersView;
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class FetchCustomersPresenter
        extends MviBasePresenter<FetchCustomersView, FetchCustomersViewState> {

    private final FetchCustomersUseCase fetchCustomersUseCase;

    public FetchCustomersPresenter(Context context) {
        this.fetchCustomersUseCase = new FetchCustomersUseCase(context);
    }

    @Override
    protected void bindIntents() {
        //Here, with aid of the Mosby library, we close our MVI cycle, achieving
        // unidirectional data flow with the use case business logic as the single
        // source of truth, everything else reflecting its state.
        final Observable<FetchCustomersViewState> fetchCustomers = intent(
            new ViewIntentBinder<FetchCustomersView, FetchCustomersAction>() {
                @NonNull
                @Override
                public Observable<FetchCustomersAction> bind(@NonNull FetchCustomersView view) {
                    return view.fetchCustomersIntent();
                }
            }
        )
        .switchMap(new Function<FetchCustomersAction, ObservableSource<FetchCustomersViewState>>() {
            @Override
            public ObservableSource<FetchCustomersViewState> apply(FetchCustomersAction action) {
                return fetchCustomersUseCase.doFetchCustomers(action)
                    .subscribeOn(Schedulers.io());
            }
        })
        .observeOn(AndroidSchedulers.mainThread());

        subscribeViewState(fetchCustomers, new ViewStateConsumer<FetchCustomersView, FetchCustomersViewState>() {
            @Override
            public void accept(@NonNull FetchCustomersView view, @NonNull FetchCustomersViewState state) {
                view.render(state);
            }
        });
    }

}
