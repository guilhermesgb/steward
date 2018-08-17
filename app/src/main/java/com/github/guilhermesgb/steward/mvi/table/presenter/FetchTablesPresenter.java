package com.github.guilhermesgb.steward.mvi.table.presenter;

import android.content.Context;
import android.support.annotation.NonNull;

import com.github.guilhermesgb.steward.mvi.table.FetchTablesUseCase;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.view.FetchTablesView;
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter;

import java.util.LinkedList;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

import static com.github.guilhermesgb.steward.network.ApiResource.WILL_USE_REAL_API;

public class FetchTablesPresenter
        extends MviBasePresenter<FetchTablesView, FetchTablesViewState> {

    private final FetchTablesUseCase fetchTablesUseCase;

    public FetchTablesPresenter(Context context) {
        this.fetchTablesUseCase = new FetchTablesUseCase(WILL_USE_REAL_API, context);
    }

    @Override
    protected void bindIntents() {
        //Here, with aid of the Mosby library, we close our MVI cycle, achieving
        // unidirectional data flow with the use case business logic as the single
        // source of truth, everything else reflecting its state.
        final Observable<FetchTablesViewState> fetchTables = intent(
                new ViewIntentBinder<FetchTablesView, FetchTablesAction>() {
                    @NonNull
                    @Override
                    public Observable<FetchTablesAction> bind(@NonNull FetchTablesView view) {
                        return view.fetchTablesIntent();
                    }
                }
        )
        .switchMap(new Function<FetchTablesAction, ObservableSource<FetchTablesViewState>>() {
            @Override
            public ObservableSource<FetchTablesViewState> apply(FetchTablesAction action) {
                return fetchTablesUseCase.doFetchTables(action)
                    .subscribeOn(Schedulers.io());
            }
        })
        .observeOn(AndroidSchedulers.mainThread());

        subscribeViewState(fetchTables
            .startWith(new FetchTablesViewState.Initial(new LinkedList<Table>())),
            new ViewStateConsumer<FetchTablesView, FetchTablesViewState>() {
                @Override
                public void accept(@NonNull FetchTablesView view, @NonNull FetchTablesViewState state) {
                    view.render(state);
                }
            });
    }

}
