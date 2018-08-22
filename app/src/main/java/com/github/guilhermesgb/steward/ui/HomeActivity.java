package com.github.guilhermesgb.steward.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.transition.TransitionManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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
import com.github.guilhermesgb.steward.utils.Argument;
import com.github.guilhermesgb.steward.utils.ArgumentsParsedCallback;
import com.github.guilhermesgb.steward.utils.ArgumentsParserMviActivity;
import com.github.guilhermesgb.steward.utils.BasicPrototypeRenderer;
import com.github.guilhermesgb.steward.utils.FontAwesomeSolid;
import com.github.guilhermesgb.steward.utils.RendererBuilderFactory;
import com.github.guilhermesgb.steward.utils.RendererItemView;
import com.joanzapata.iconify.IconDrawable;
import com.pedrogomez.renderers.ListAdapteeCollection;
import com.pedrogomez.renderers.RVRendererAdapter;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;
import io.reactivex.subjects.PublishSubject;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.github.guilhermesgb.steward.utils.ArgumentsParser.WILL_NOT_DEFINE_ARGUMENTS;
import static com.github.guilhermesgb.steward.utils.ArgumentsParser.WILL_NOT_DEFINE_RESOLVED_ARGUMENT_VALUES_ASSIGNER;

public class HomeActivity
        extends ArgumentsParserMviActivity<MakeReservationsView, MakeReservationsPresenter>
            implements MakeReservationsView {

    public static final String BUNDLE_KEY_LAST_STATE = "LAST_STATE";

    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER_SPACE = 0;
    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER_LOADING = 1;
    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER = 2;
    private static final int RENDERER_ITEM_VIEW_CODE_CUSTOMER_WITH_DIVIDER = 3;
    private static final int RENDERER_ITEM_VIEW_CODE_TABLE = 4;
    private static final int RENDERER_ITEM_VIEW_CODE_TABLE_SPACE = 5;
    private static final int RENDERER_ITEM_VIEW_CODE_TABLE_CONFIRM = 6;

    @BindView(R.id.contentView) ViewGroup contentView;
    @BindView(R.id.toolbarView) Toolbar toolbarView;
    @BindView(R.id.customersSearchView) SearchView customersSearchView;
    @BindView(R.id.recyclerViews) ViewGroup recyclerViews;
    @BindView(R.id.customersView) RecyclerView customersView;
    @BindView(R.id.tablesView) RecyclerView tablesView;
    @BindView(R.id.errorFeedbackViewWrapper) ViewGroup errorFeedbackViewWrapper;
    @BindView(R.id.errorFeedbackDivider) View errorFeedbackDivider;
    @BindView(R.id.errorFeedbackBackground) View errorFeedbackBackground;
    @BindView(R.id.errorFeedbackView) View errorFeedbackView;
    @BindView(R.id.errorFeedbackIcon) ImageView errorFeedbackIcon;
    @BindView(R.id.errorFeedbackText) TextView errorFeedbackText;

    private RVRendererAdapter<RendererItemView> customersItemViewsAdapter;
    private ListAdapteeCollection<RendererItemView> customersItemViews = new ListAdapteeCollection<>();
    private ArrayAdapter<CustomerItemView> customersSearchItemViewsAdapter;
    private List<CustomerItemView> customersSearchItemViews = new LinkedList<>();
    private RVRendererAdapter<RendererItemView> tablesItemViewsAdapter;
    private ListAdapteeCollection<RendererItemView> tablesItemViews = new ListAdapteeCollection<>();

    private MakeReservationsViewState lastRenderedState;

    PublishSubject<FetchCustomersAction> fetchCustomersActions = PublishSubject.create();
    PublishSubject<ChooseCustomerAction> chooseCustomerActions = PublishSubject.create();
    PublishSubject<FetchTablesAction> fetchTablesActions = PublishSubject.create();
    PublishSubject<ChooseTableAction> chooseTableActions = PublishSubject.create();
    PublishSubject<ConfirmReservationAction> confirmReservationActions = PublishSubject.create();

    @Override
    protected List<Argument> defineExpectedArguments() {
        return WILL_NOT_DEFINE_ARGUMENTS;
    }

    @Override
    protected ArgumentsParsedCallback defineResolvedArgumentValuesAssigner() {
        return WILL_NOT_DEFINE_RESOLVED_ARGUMENT_VALUES_ASSIGNER;
    }

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
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, 2);
        gridLayoutManager.setItemPrefetchEnabled(false);
        tablesView.setLayoutManager(gridLayoutManager);
        tablesView.setAdapter(tablesItemViewsAdapter);
        SearchViewHolder searchViewHolder = new SearchViewHolder();
        ButterKnife.bind(searchViewHolder, customersSearchView);
        if (searchViewHolder.searchIcon != null) {
            searchViewHolder.searchIcon.setImageDrawable
                (new IconDrawable(this, FontAwesomeSolid.fa_s_search)
                    .colorRes(android.R.color.white)
                    .actionBarSize());
        }
        if (searchViewHolder.closeIcon != null) {
            searchViewHolder.closeIcon.setImageDrawable
                (new IconDrawable(this, FontAwesomeSolid.fa_s_times_circle)
                    .colorRes(android.R.color.white)
                    .actionBarSize());
        }
        customersSearchView.setQuery("", false);
        customersSearchView.setIconified(true);
        customersSearchItemViewsAdapter = new ArrayAdapter<>(this,
            R.layout.adapter_customer, customersSearchItemViews);
        searchViewHolder.searchAutoComplete.setAdapter(customersSearchItemViewsAdapter);
        searchViewHolder.searchAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Customer customer = ((CustomerItemView) parent
                    .getItemAtPosition(position)).getCustomer();
                final List<Customer> cachedCustomers = new LinkedList<>();
                for (int i=0; i<parent.getCount(); i++) {
                    cachedCustomers.add(((CustomerItemView) parent
                        .getItemAtPosition(i)).getCustomer());
                }
                customersSearchView.setQuery(customer.getFirstName()
                    + " " + customer.getLastName(), false);
                lastRenderedState = null;
                chooseCustomerActions.onNext(new ChooseCustomerAction
                    (new FetchCustomersViewState.SuccessFetchingCustomers
                        (new FetchCustomersAction(), cachedCustomers),
                            customer));
            }
        });
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
        customersSearchItemViews.clear();
        for (Customer customer : substate.getCustomers()) {
            customersSearchItemViews.add(new CustomerItemView
                (customer, null) {
                    @Override
                    public int getItemViewCode() {
                        return RENDERER_ITEM_VIEW_CODE_CUSTOMER;
                    }
                }
            );
        }
        customersSearchItemViewsAdapter.notifyDataSetChanged();
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
            customersSearchView.setQuery("", false);
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
                        if (chosenCustomer == null) {
                            chooseCustomerActions.onNext
                                (new ChooseCustomerAction
                                    (substate, customer));
                        } else {
                            lastRenderedState = null;
                            fetchCustomersActions.onNext
                                (new FetchCustomersAction());
                        }
                    }
                }
            ) {
                @Override
                public int getItemViewCode() {
                    return rendererItemViewCode;
                }
            });
        }
        if (customers.isEmpty()) {
            customersItemViews.add(new CustomerItemView(new Customer("-1",
                getString(R.string.label_no_customers_found), ""),
                    new CustomerItemView.CustomerChosenCallback() {
                        @Override
                        public void onCustomerChosen(Customer customer) {
                            fetchCustomersActions.onNext
                                (new FetchCustomersAction());
                        }
                    }
                ) {
                    @Override
                    public int getItemViewCode() {
                        return RENDERER_ITEM_VIEW_CODE_CUSTOMER_WITH_DIVIDER;
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
        TransitionManager.beginDelayedTransition(tablesView);
        List<Table> tables = substate.getTables();
        if (chosenTable != null) {
            tables = Collections.singletonList(chosenTable);
        }
        int amountBeforeThisIteration = tablesItemViews.size();
        tablesItemViews.clear();
        for (Table table : tables) {
            tablesItemViews.add(new TableItemView(table,
                new TableItemView.TableChosenCallback() {
                    @Override
                    public void onTableChosen(Table table) {
                        if (chosenTable == null) {
                            chooseTableActions.onNext(new ChooseTableAction
                                (previousState, substate, table));
                        } else {
                            lastRenderedState = null;
                            fetchTablesActions.onNext
                                (new FetchTablesAction());
                        }
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
            tablesItemViewsAdapter.notifyItemMoved(chosenTable.getNumber(), 0);
        } else {
            tablesItemViews.add(new RendererItemView() {
                @Override
                public int getItemViewCode() {
                    return RENDERER_ITEM_VIEW_CODE_TABLE_SPACE;
                }
            });
        }
        if (tables.isEmpty()) {
            showFeedbackView(getString(R.string.label_no_tables_found), true);
        } else {
            if (errorFeedbackViewWrapper.getVisibility() == GONE) {
                hideErrorFeedbackViewPartially();
            }
        }
        if (chosenTable != null) {
            for (int i=0; i<tablesItemViews.size(); i++) {
                RendererItemView rendererItemView = tablesItemViews.get(i);
                if (rendererItemView instanceof TableItemView) {
                    if (((TableItemView) rendererItemView).getTable().getNumber() == chosenTable.getNumber()) {
                        tablesItemViewsAdapter.notifyItemRangeRemoved(1, i);
                        tablesItemViewsAdapter.notifyItemChanged(i);
                        tablesItemViewsAdapter.notifyItemRangeRemoved(i + 1, amountBeforeThisIteration);
                        break;
                    }
                }
            }
        } else {
            tablesItemViewsAdapter.notifyDataSetChanged();
        }
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
