package com.github.guilhermesgb.steward.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.guilhermesgb.steward.R;
import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseCustomerAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseTableAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ConfirmReservationAction;
import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;
import com.github.guilhermesgb.steward.mvi.reservation.presenter.MakeReservationsPresenter;
import com.github.guilhermesgb.steward.mvi.reservation.view.MakeReservationsView;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.utils.BasicPrototypeRenderer;
import com.github.guilhermesgb.steward.utils.FontAwesomeSolid;
import com.github.guilhermesgb.steward.utils.RendererBuilderFactory;
import com.github.guilhermesgb.steward.utils.RendererItemView;
import com.hannesdorfmann.mosby3.mvi.MviActivity;
import com.joanzapata.iconify.IconDrawable;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import timber.log.Timber;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class HomeActivity
        extends MviActivity<MakeReservationsView, MakeReservationsPresenter>
            implements MakeReservationsView {

    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER_SPACE = 0;
    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER_LOADING = 1;
    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER = 2;
    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER_WITH_DIVIDER = 3;
    private static final int RENDERER_ITEM_VIEW_CODE_TABLE = 4;
    private static final int RENDERER_ITEM_VIEW_CODE_TABLE_SPACE = 5;
    private static final int RENDERER_ITEM_VIEW_CODE_TABLE_CONFIRM = 6;

    @BindView(R.id.contentView) ViewGroup contentView;
    @BindView(R.id.toolbarView) Toolbar toolbarView;
    @BindView(R.id.customersView) RecyclerView customersView;
    @BindView(R.id.tablesView) RecyclerView tablesView;
    @BindView(R.id.errorFeedbackViewWrapper) View errorFeedbackViewWrapper;
    @BindView(R.id.errorFeedbackDivider) View errorFeedbackDivider;
    @BindView(R.id.errorFeedbackBackground) View errorFeedbackBackground;
    @BindView(R.id.errorFeedbackView) View errorFeedbackView;
    @BindView(R.id.errorFeedbackIcon) ImageView errorFeedbackIcon;
    @BindView(R.id.errorFeedbackText) TextView errorFeedbackText;

    private RVRendererAdapter<RendererItemView> customersItemViewsAdapter;
    private ListAdapteeCollection<RendererItemView> customersItemViews = new ListAdapteeCollection<>();
    private RVRendererAdapter<RendererItemView> tablesItemViewsAdapter;
    private ListAdapteeCollection<RendererItemView> tablesItemViews = new ListAdapteeCollection<>();

    private MakeReservationsViewState lastRenderedState;

    PublishSubject<FetchCustomersAction> fetchCustomersActions = PublishSubject.create();
    PublishSubject<ChooseCustomerAction> chooseCustomerActions = PublishSubject.create();
    PublishSubject<FetchTablesAction> fetchTablesActions = PublishSubject.create();
    PublishSubject<ChooseTableAction> chooseTableActions = PublishSubject.create();
    PublishSubject<ConfirmReservationAction> confirmReservationActions = PublishSubject.create();

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        ButterKnife.bind(this);
        setSupportActionBar(toolbarView);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);
            actionBar.setDisplayShowTitleEnabled(false);
        }
        RendererBuilder<RendererItemView> customersRendererBuilder = new RendererBuilderFactory<>()
            .bind(RENDERER_ITEM_VIEW_CODE_CUSTOMER_SPACE, new BasicPrototypeRenderer() {
                @Override
                public int getPrototypeResourceId() {
                    return R.layout.renderer_customer_space;
                }
            })
            .bind(RENDERER_ITEM_VIEW_CODE_CUSTOMER_LOADING, new BasicPrototypeRenderer() {
                @Override
                public int getPrototypeResourceId() {
                    return R.layout.renderer_customer_loading;
                }
            })
            .bind(RENDERER_ITEM_VIEW_CODE_CUSTOMER, new CustomerRenderer())
            .bind(RENDERER_ITEM_VIEW_CODE_CUSTOMER_WITH_DIVIDER, new CustomerDividerRenderer())
            .build();
        customersItemViewsAdapter = new RVRendererAdapter<>(customersRendererBuilder, customersItemViews);
        customersView.setLayoutManager(new LinearLayoutManager(this));
        customersView.setAdapter(customersItemViewsAdapter);
        RendererBuilder<RendererItemView> tablesRendererBuilder = new RendererBuilderFactory<>()
            .bind(RENDERER_ITEM_VIEW_CODE_TABLE, new TableRenderer())
            .bind(RENDERER_ITEM_VIEW_CODE_TABLE_SPACE, new BasicPrototypeRenderer() {
                @Override
                public int getPrototypeResourceId() {
                    return R.layout.renderer_table_space;
                }
            })
            .bind(RENDERER_ITEM_VIEW_CODE_TABLE_CONFIRM, new TableConfirmRenderer())
            .build();
        tablesItemViewsAdapter = new RVRendererAdapter<>(tablesRendererBuilder, tablesItemViews);
        tablesView.setLayoutManager(new GridLayoutManager(this, 2));
        tablesView.setAdapter(tablesItemViewsAdapter);
    }

    @NonNull
    @Override
    public MakeReservationsPresenter createPresenter() {
        return new MakeReservationsPresenter(getApplicationContext());
    }

    @Override
    public Observable<FetchCustomersAction> fetchCustomersIntent() {
        return fetchCustomersActions;
    }

    @Override
    public Observable<ChooseCustomerAction> chooseCustomerIntent() {
        return chooseCustomerActions;
    }

    @Override
    public Observable<FetchTablesAction> fetchTablesIntent() {
        return fetchTablesActions;
    }

    @Override
    public Observable<ChooseTableAction> chooseTableIntent() {
        return chooseTableActions;
    }

    @Override
    public Observable<ConfirmReservationAction> confirmReservationIntent() {
        return confirmReservationActions;
    }

    @Override
    public void onReady() {
        fetchCustomersActions.onNext(new FetchCustomersAction());
        fetchTablesActions.onNext(new FetchTablesAction());
    }

    public void renderCustomersSubstate(FetchCustomersViewState state) {
        state.continued(new Consumer<FetchCustomersViewState.Initial>() {
            @Override
            public void accept(FetchCustomersViewState.Initial initial) {
                renderCustomers(new FetchCustomersViewState.SuccessFetchingCustomers
                    (new FetchCustomersAction(), initial.getCachedCustomers()),
                        null);
            }
        }, new Consumer<FetchCustomersViewState.FetchingCustomers>() {
            @Override
            public void accept(FetchCustomersViewState.FetchingCustomers loading) {
                renderLoadingCustomers();
            }
        }, new Consumer<FetchCustomersViewState.SuccessFetchingCustomers>() {
            @Override
            public void accept(FetchCustomersViewState.SuccessFetchingCustomers success) {
                renderCustomers(success, null);
            }
        }, new Consumer<FetchCustomersViewState.ErrorFetchingCustomers>() {
            @Override
            public void accept(FetchCustomersViewState.ErrorFetchingCustomers error) {
                renderCustomers(new FetchCustomersViewState.SuccessFetchingCustomers
                    (new FetchCustomersAction(), error.getCachedCustomers()),
                        null);
            }
        });
    }

    public void renderTablesSubstate(final MakeReservationsViewState.CustomerChosen previousState,
                                     FetchTablesViewState state) {
        state.continued(new Consumer<FetchTablesViewState.Initial>() {
            @Override
            public void accept(FetchTablesViewState.Initial initial) {
                renderTables(previousState, new FetchTablesViewState.SuccessFetchingTables
                    (new FetchTablesAction(), initial.getCachedTables()), null);
            }
        }, new Consumer<FetchTablesViewState.FetchingTables>() {
            @Override
            public void accept(FetchTablesViewState.FetchingTables loading) {
                renderLoadingTables();
            }
        }, new Consumer<FetchTablesViewState.SuccessFetchingTables>() {
            @Override
            public void accept(FetchTablesViewState.SuccessFetchingTables success) {
                renderTables(previousState, success, null);
            }
        }, new Consumer<FetchTablesViewState.ErrorFetchingTables>() {
            @Override
            public void accept(FetchTablesViewState.ErrorFetchingTables error) {
                renderTables(previousState, new FetchTablesViewState.SuccessFetchingTables
                    (new FetchTablesAction(), error.getCachedTables()), null);
            }
        });
    }

    @Override
    public void render(final MakeReservationsViewState state) {
//        TransitionManager.beginDelayedTransition(contentView);
        state.continued(new Consumer<MakeReservationsViewState.Initial>() {
            @Override
            public void accept(MakeReservationsViewState.Initial initial) {
                renderCustomersSubstate(initial.getSubstate());
                updateLastRenderedState(initial);
            }
        }, new Consumer<MakeReservationsViewState.CustomerChosen>() {
            @Override
            public void accept(MakeReservationsViewState.CustomerChosen customerChosen) {
                renderCustomers(customerChosen.getFirstSubstate(),
                    customerChosen.getChosenCustomer());
                renderTablesSubstate(customerChosen, customerChosen.getSecondSubstate());
                updateLastRenderedState(customerChosen);
            }
        }, new Consumer<MakeReservationsViewState.TableChosen>() {
            @Override
            public void accept(MakeReservationsViewState.TableChosen tableChosen) {
                renderCustomers(tableChosen.getFirstSubstate().getFirstSubstate(),
                    tableChosen.getFirstSubstate().getChosenCustomer());
                renderTables(tableChosen.getFirstSubstate(), tableChosen
                    .getSecondSubstate(), tableChosen);
                updateLastRenderedState(tableChosen);
            }
        }, new Consumer<MakeReservationsViewState.MakingReservation>() {
            @Override
            public void accept(MakeReservationsViewState.MakingReservation loading) {
                updateLastRenderedState(loading);
            }
        }, new Consumer<MakeReservationsViewState.SuccessMakingReservation>() {
            @Override
            public void accept(MakeReservationsViewState.SuccessMakingReservation success) {
                showFeedbackView(getString(R.string.format_success_reservation_in_place,
                    success.getFinalSubstate().getFirstSubstate().getChosenCustomer().getFirstName(),
                    success.getFinalSubstate().getChosenTable().getNumber()), false);
                updateLastRenderedState(success);
            }
        }, new Consumer<MakeReservationsViewState.ErrorMakingReservation>() {
            @Override
            public void accept(MakeReservationsViewState.ErrorMakingReservation error) {
                final String errorMessage;
                //noinspection ThrowableNotThrown
                switch (error.getException().getCode()) {
                    case CUSTOMER_NOT_FOUND:
                        errorMessage = getString(R.string.format_error_customer_not_found,
                            error.getFinalSubstate().getFirstSubstate().getChosenCustomer().getFirstName());
                        break;
                    case CUSTOMER_BUSY:
                        //noinspection unchecked
                        errorMessage = getString(R.string.format_error_customer_busy,
                            error.getFinalSubstate().getFirstSubstate().getChosenCustomer().getFirstName(),
                            ((List<Table>) error.getPayload()).get(0).getNumber());
                        break;
                    case RESERVATION_IN_PLACE:
                        errorMessage = getString(R.string.format_error_reservation_in_place,
                            error.getFinalSubstate().getFirstSubstate().getChosenCustomer().getFirstName(),
                            error.getFinalSubstate().getChosenTable().getNumber());
                        break;
                    case TABLE_NOT_FOUND:
                        errorMessage = getString(R.string.format_error_table_not_found,
                            error.getFinalSubstate().getChosenTable().getNumber());
                        break;
                    case TABLE_UNAVAILABLE:
                        errorMessage = getString(R.string.format_error_table_unavailable,
                            error.getFinalSubstate().getChosenTable().getNumber());
                        break;
                    case DATABASE_FAILURE:
                        errorMessage = getString(R.string.format_error_database_confirm_reservation);
                        break;
                    default:
                        errorMessage = getString(R.string.format_error_unknown_confirm_reservation);
                }
                showFeedbackView(errorMessage, true);
                updateLastRenderedState(error);
            }
        });
    }

    private void updateLastRenderedState(MakeReservationsViewState state) {
        if (lastRenderedState == null) {
            lastRenderedState = state;
        } else if (state.getPrecedenceValue() >= lastRenderedState.getPrecedenceValue()) {
            lastRenderedState = state;
            Timber.wtf("lastRenderedState updated to %s", state);
        }
    }

    private void renderLoadingCustomers() {
        customersItemViews.clear();
        customersItemViews.add(new RendererItemView() {
            @Override
            public int getItemViewCode() {
                return RENDERER_ITEM_VIEW_CODE_CUSTOMER_SPACE;
            }
        });
        customersItemViews.add(new RendererItemView() {
            @Override
            public int getItemViewCode() {
                return RENDERER_ITEM_VIEW_CODE_CUSTOMER_LOADING;
            }
        });
        customersItemViewsAdapter.notifyDataSetChanged();
    }

    private void renderCustomers(final FetchCustomersViewState.SuccessFetchingCustomers substate,
                                 final Customer chosenCustomer) {
        //FIXME add customers to an adapter that shall feed search suggestions.
        if (lastRenderedState != null && chosenCustomer == null) {
            lastRenderedState.continued(null, new Consumer<MakeReservationsViewState.CustomerChosen>() {
                @Override
                public void accept(MakeReservationsViewState.CustomerChosen customerChosen) {
                    renderCustomers(substate, customerChosen.getChosenCustomer());
                }
            }, new Consumer<MakeReservationsViewState.TableChosen>() {
                @Override
                public void accept(MakeReservationsViewState.TableChosen tableChosen) {
                    renderCustomers(substate, tableChosen.getFirstSubstate().getChosenCustomer());
                }
            }, new Consumer<MakeReservationsViewState.MakingReservation>() {
                @Override
                public void accept(MakeReservationsViewState.MakingReservation loading) {
                    renderCustomers(substate, loading.getFinalSubstate()
                        .getFirstSubstate().getChosenCustomer());
                }
            }, new Consumer<MakeReservationsViewState.SuccessMakingReservation>() {
                @Override
                public void accept(MakeReservationsViewState.SuccessMakingReservation success) {
                    renderCustomers(substate, success.getFinalSubstate()
                        .getFirstSubstate().getChosenCustomer());
                }
            }, new Consumer<MakeReservationsViewState.ErrorMakingReservation>() {
                @Override
                public void accept(MakeReservationsViewState.ErrorMakingReservation error) {
                    renderCustomers(substate, error.getFinalSubstate()
                        .getFirstSubstate().getChosenCustomer());
                }
            });
            if (!(lastRenderedState instanceof MakeReservationsViewState.Initial)) {
                return;
            }
        }
        customersItemViews.clear();
        List<Customer> customers = substate.getCustomers();
        customersItemViews.add(new RendererItemView() {
            @Override
            public int getItemViewCode() {
                return RENDERER_ITEM_VIEW_CODE_CUSTOMER_SPACE;
            }
        });
        if (chosenCustomer != null) {
            customers = Collections.singletonList(chosenCustomer);
        }
        for (int i=0; i<customers.size(); i++) {
            final int rendererItemViewCode;
            if (i == customers.size() - 1) {
                rendererItemViewCode = RENDERER_ITEM_VIEW_CODE_CUSTOMER_WITH_DIVIDER;
            } else {
                rendererItemViewCode = RENDERER_ITEM_VIEW_CODE_CUSTOMER;
            }
            customersItemViews.add(new CustomerItemView(customers.get(i),
                new CustomerItemView.CustomerChosenCallback() {
                    @Override
                    public void onCustomerChosen(Customer customer) {
                        chooseCustomerActions.onNext(new ChooseCustomerAction
                            (substate, customer));
                    }
                }
            ) {
                @Override
                public int getItemViewCode() {
                    return rendererItemViewCode;
                }
            });
        }
        if (chosenCustomer != null) {
            if (errorFeedbackViewWrapper.getVisibility() == GONE) {
                hideErrorFeedbackViewPartially();
            }
        } else {
            hideErrorFeedbackViewCompletely();
        }
        customersItemViewsAdapter.notifyDataSetChanged();
    }

    private void renderLoadingTables() {
        tablesItemViews.clear();
        tablesItemViewsAdapter.notifyDataSetChanged();
    }

    private void renderTables(final MakeReservationsViewState.CustomerChosen previousState,
                              final FetchTablesViewState.SuccessFetchingTables substate,
                              final MakeReservationsViewState.TableChosen finalState) {
        final Table chosenTable = finalState == null
            ? null : finalState.getChosenTable();
        if (lastRenderedState != null && chosenTable == null) {
            lastRenderedState.continued(
                null,
                null,
                new Consumer<MakeReservationsViewState.TableChosen>() {
                    @Override
                    public void accept(MakeReservationsViewState.TableChosen tableChosen) {
                        renderTables(previousState, substate, tableChosen);
                    }
                }, new Consumer<MakeReservationsViewState.MakingReservation>() {
                    @Override
                    public void accept(MakeReservationsViewState.MakingReservation loading) {
                        renderTables(previousState, substate, loading.getFinalSubstate());
                    }
                }, new Consumer<MakeReservationsViewState.SuccessMakingReservation>() {
                    @Override
                    public void accept(MakeReservationsViewState.SuccessMakingReservation success) {
                        renderTables(previousState, substate, success.getFinalSubstate());
                    }
                }, new Consumer<MakeReservationsViewState.ErrorMakingReservation>() {
                    @Override
                    public void accept(MakeReservationsViewState.ErrorMakingReservation error) {
                        renderTables(previousState, substate, error.getFinalSubstate());
                    }
                }
            );
            if (!(lastRenderedState instanceof MakeReservationsViewState.Initial
                    || lastRenderedState instanceof MakeReservationsViewState.CustomerChosen)) {
                return;
            }
        }
        List<Table> tables = substate.getTables();
        if (chosenTable != null) {
            tables = Collections.singletonList(chosenTable);
        }
        tablesItemViews.clear();
        for (Table table : tables) {
            tablesItemViews.add(new TableItemView(table,
                new TableItemView.TableChosenCallback() {
                    @Override
                    public void onTableChosen(Table table) {
                        chooseTableActions.onNext(new ChooseTableAction
                            (previousState, substate, table));
                    }
                }
            ) {
                @Override
                public int getItemViewCode() {
                    return RENDERER_ITEM_VIEW_CODE_TABLE;
                }
            });
        }
        if (chosenTable != null) {
            tablesItemViews.add(new TableItemView(chosenTable,
                new TableItemView.TableChosenCallback() {
                    @Override
                    public void onTableChosen(Table table) {
                        confirmReservationActions.onNext
                            (new ConfirmReservationAction(finalState));
                    }
                }
            ) {
                @Override
                public int getItemViewCode() {
                    return RENDERER_ITEM_VIEW_CODE_TABLE_CONFIRM;
                }
            });
        } else {
            tablesItemViews.add(new RendererItemView() {
                @Override
                public int getItemViewCode() {
                    return RENDERER_ITEM_VIEW_CODE_TABLE_SPACE;
                }
            });
        }
        tablesItemViewsAdapter.notifyDataSetChanged();
    }

    private void hideErrorFeedbackViewCompletely() {
        errorFeedbackViewWrapper.setVisibility(GONE);
    }

    private void hideErrorFeedbackViewPartially() {
        errorFeedbackDivider.setVisibility(VISIBLE);
        errorFeedbackBackground.setVisibility(GONE);
        errorFeedbackIcon.setVisibility(GONE);
        errorFeedbackText.setVisibility(GONE);
        errorFeedbackView.setVisibility(GONE);
        errorFeedbackViewWrapper.setVisibility(VISIBLE);
    }

    private void showFeedbackView(String message, boolean isError) {
        errorFeedbackDivider.setVisibility(VISIBLE);
        errorFeedbackBackground.setVisibility(VISIBLE);
        if (isError) {
            errorFeedbackIcon.setImageDrawable(new IconDrawable
                (getApplicationContext(), FontAwesomeSolid.fa_s_exclamation_triangle)
                    .colorRes(R.color.colorAccent).sizeDp(30));
            errorFeedbackText.setTextColor(ContextCompat.getColor
                (getApplicationContext(), R.color.colorAccent));
        } else {
            errorFeedbackIcon.setImageDrawable(new IconDrawable
                (getApplicationContext(), FontAwesomeSolid.fa_s_check_circle)
                    .colorRes(R.color.colorAccentSecondary).sizeDp(30));
            errorFeedbackText.setTextColor(ContextCompat.getColor
                (getApplicationContext(), R.color.colorAccentSecondary));
        }
        errorFeedbackIcon.setVisibility(VISIBLE);
        errorFeedbackText.setText(message);
        errorFeedbackText.setVisibility(VISIBLE);
        errorFeedbackView.setVisibility(VISIBLE);
        errorFeedbackViewWrapper.setVisibility(VISIBLE);
    }

}
