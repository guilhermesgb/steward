package com.github.guilhermesgb.steward.reservation;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.mvi.customer.FetchCustomersUseCase;
import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.customer.schema.CustomerDao;
import com.github.guilhermesgb.steward.mvi.reservation.MakeReservationsUseCase;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseCustomerAction;
import com.github.guilhermesgb.steward.mvi.reservation.intent.ChooseTableAction;
import com.github.guilhermesgb.steward.mvi.reservation.model.MakeReservationsViewState;
import com.github.guilhermesgb.steward.mvi.table.FetchTablesUseCase;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.schema.TableDao;
import com.github.guilhermesgb.steward.network.ApiEndpoints;
import com.github.guilhermesgb.steward.utils.IterableUtils;
import com.github.guilhermesgb.steward.utils.MockedServerUnitTest;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.LinkedList;
import java.util.List;

import io.reactivex.Single;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.http.GET;

import static com.github.guilhermesgb.steward.utils.StringUtils.isEmpty;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MakeReservationsUseCaseTest extends MockedServerUnitTest {

    @Test
    public void makeReservations_noLocalCustomers_remoteCustomers_chooseRemoteCustomer_shouldYieldThatCustomer() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four customers below.
        String remoteCustomers = "[\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Marilyn\",\n" +
            "    \"customerLastName\": \"Monroe\",\n" +
            "    \"id\": 0\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Abraham\",\n" +
            "    \"customerLastName\": \"Lincoln\",\n" +
            "    \"id\": 1\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Mother\",\n" +
            "    \"customerLastName\": \"Teresa\",\n" +
            "    \"id\": 2\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"John F.\",\n" +
            "    \"customerLastName\": \"Kennedy\",\n" +
            "    \"id\": 3\n" +
            "  }\n" +
            "]";
        //Setting up database to return an empty list of customers;
        List<Customer> localCustomers = new LinkedList<>();

        setupMakeReservationsTest(localCustomers, remoteCustomers, new MakeReservationsTestSetupCallback() {
            @Override
            public void onSetupComplete(MockWebServer server,
                                        CustomerDao customerDaoMock,
                                        TableDao tableDaoMock,
                                        MakeReservationsUseCase makeReservationsUseCase,
                                        FetchCustomersViewState.SuccessFetchingCustomers localSuccessFetchingCustomers,
                                        FetchCustomersViewState.SuccessFetchingCustomers remoteSuccessFetchingCustomers) throws Exception {

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(remoteSuccessFetchingCustomers.getCustomers(), hasSize(4));
                assertThat(remoteSuccessFetchingCustomers.getCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                // ### EXECUTION PHASE ###

                Customer motherTheresa = remoteSuccessFetchingCustomers.getCustomers().get(2);
                ChooseCustomerAction action = new ChooseCustomerAction(remoteSuccessFetchingCustomers, motherTheresa);

                final List<MakeReservationsViewState> states = new LinkedList<>();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseCustomer(action).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen loading = (MakeReservationsViewState.CustomerChosen) states.get(0);
                assertThat(loading.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(loading.getSecondSubstate(), instanceOf(FetchTablesViewState.FetchingTables.class));

                assertThat(states.get(1), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen localSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(1);
                assertThat(localSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(localSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables localSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) localSuccessFetchingTables.getSecondSubstate();
                assertThat(localSuccessSubstate.getTables(), hasSize(0));

                assertThat(states.get(2), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen remoteSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(2);
                assertThat(remoteSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables remoteSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) remoteSuccessFetchingTables.getSecondSubstate();
                assertThat(remoteSuccessSubstate.getTables(), hasSize(0));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(2));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock).deleteAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("0", "Marilyn", "Monroe"));
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("2", "Mother", "Teresa"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(tableDaoMock).findAll();
                verify(tableDaoMock).deleteAll();
                verify(tableDaoMock).insertAll(new LinkedList<Table>());
            }
        });
    }

    @Test
    public void makeReservations_someLocalCustomers_chooseLocalCustomer_chosenCustomerNotInRemoteCustomers_shouldYieldThatCustomerNonetheless() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these two customers below.
        String remoteCustomers = "[\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Abraham\",\n" +
            "    \"customerLastName\": \"Lincoln\",\n" +
            "    \"id\": 1\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"John F.\",\n" +
            "    \"customerLastName\": \"Kennedy\",\n" +
            "    \"id\": 3\n" +
            "  }\n" +
            "]";
        //Setting up database to return these two other customers below;
        List<Customer> localCustomers = new LinkedList<>();
        localCustomers.add(new Customer("2", "Mother", "Teresa"));
        localCustomers.add(new Customer("0", "Marilyn", "Monroe"));

        setupMakeReservationsTest(localCustomers, remoteCustomers, new MakeReservationsTestSetupCallback() {
            @Override
            public void onSetupComplete(MockWebServer server,
                                        CustomerDao customerDaoMock,
                                        TableDao tableDaoMock,
                                        MakeReservationsUseCase makeReservationsUseCase,
                                        FetchCustomersViewState.SuccessFetchingCustomers localSuccessFetchingCustomers,
                                        FetchCustomersViewState.SuccessFetchingCustomers remoteSuccessFetchingCustomers) throws Exception {

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(localSuccessFetchingCustomers.getCustomers(), hasSize(2));
                assertThat(localSuccessFetchingCustomers.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                // ### EXECUTION PHASE ###

                Customer motherTheresa = localSuccessFetchingCustomers.getCustomers().get(0);
                ChooseCustomerAction action = new ChooseCustomerAction(localSuccessFetchingCustomers, motherTheresa);

                final List<MakeReservationsViewState> states = new LinkedList<>();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseCustomer(action).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen loading = (MakeReservationsViewState.CustomerChosen) states.get(0);
                assertThat(loading.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(loading.getSecondSubstate(), instanceOf(FetchTablesViewState.FetchingTables.class));

                assertThat(states.get(1), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen localSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(1);
                assertThat(localSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(localSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables localSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) localSuccessFetchingTables.getSecondSubstate();
                assertThat(localSuccessSubstate.getTables(), hasSize(0));

                assertThat(states.get(2), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen remoteSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(2);
                assertThat(remoteSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables remoteSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) remoteSuccessFetchingTables.getSecondSubstate();
                assertThat(remoteSuccessSubstate.getTables(), hasSize(0));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(2));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock).deleteAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(tableDaoMock).findAll();
                verify(tableDaoMock).deleteAll();
                verify(tableDaoMock).insertAll(new LinkedList<Table>());
            }
        });
    }

    @Test
    public void makeReservations_noLocalTables_remoteTables_chooseRemoteAvailableTable_shouldYieldThatTable() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four customers below.
        String remoteCustomers = "[\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Marilyn\",\n" +
            "    \"customerLastName\": \"Monroe\",\n" +
            "    \"id\": 0\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Abraham\",\n" +
            "    \"customerLastName\": \"Lincoln\",\n" +
            "    \"id\": 1\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Mother\",\n" +
            "    \"customerLastName\": \"Teresa\",\n" +
            "    \"id\": 2\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"John F.\",\n" +
            "    \"customerLastName\": \"Kennedy\",\n" +
            "    \"id\": 3\n" +
            "  }\n" +
            "]";
        //Setting up database to return these two customers below;
        List<Customer> localCustomers = new LinkedList<>();
        localCustomers.add(new Customer("2", "Mother", "Teresa"));
        localCustomers.add(new Customer("0", "Marilyn", "Monroe"));

        //Setting up mock server to return these five tables below.
        String remoteTables = "[false, true, true, false, true]";
        //Setting up database to return this empty list of tables below.
        List<Table> localTables = new LinkedList<>();

        setupMakeReservationsTest(localCustomers, remoteCustomers, localTables, remoteTables, new MakeReservationsTestSetupCallback() {
            @Override
            public void onSetupComplete(MockWebServer server,
                                        CustomerDao customerDaoMock,
                                        TableDao tableDaoMock,
                                        MakeReservationsUseCase makeReservationsUseCase,
                                        FetchCustomersViewState.SuccessFetchingCustomers localSuccessFetchingCustomers,
                                        FetchCustomersViewState.SuccessFetchingCustomers remoteSuccessFetchingCustomers) throws Exception {

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(localSuccessFetchingCustomers.getCustomers(), hasSize(2));
                assertThat(localSuccessFetchingCustomers.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                // ### PREREQUISITES EXECUTION PHASE ###

                Customer motherTheresa = localSuccessFetchingCustomers.getCustomers().get(0);
                ChooseCustomerAction prerequisiteAction = new ChooseCustomerAction(localSuccessFetchingCustomers, motherTheresa);

                final List<MakeReservationsViewState> states = new LinkedList<>();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseCustomer(prerequisiteAction).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen loading = (MakeReservationsViewState.CustomerChosen) states.get(0);
                assertThat(loading.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(loading.getSecondSubstate(), instanceOf(FetchTablesViewState.FetchingTables.class));

                assertThat(states.get(1), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen localSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(1);
                assertThat(localSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(localSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables localSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) localSuccessFetchingTables.getSecondSubstate();
                assertThat(localSuccessSubstate.getTables(), hasSize(0));

                assertThat(states.get(2), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen remoteSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(2);
                assertThat(remoteSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables remoteSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) remoteSuccessFetchingTables.getSecondSubstate();
                assertThat(remoteSuccessSubstate.getTables(), hasSize(5));
                assertThat(remoteSuccessSubstate.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));

                // ### EXECUTION PHASE ###

                Table tableNumberOne = remoteSuccessSubstate.getTables().get(1);
                ChooseTableAction action = new ChooseTableAction
                    (remoteSuccessFetchingTables, remoteSuccessSubstate, tableNumberOne);

                states.clear();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseTable(action).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(1));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.TableChosen.class));

                MakeReservationsViewState.TableChosen tableChosen
                    = (MakeReservationsViewState.TableChosen) states.get(0);
                assertThat(tableChosen.getChosenTable(), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(tableChosen.getSecondSubstate().getTables(), hasSize(5));
                assertThat(tableChosen.getSecondSubstate().getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(tableChosen.getFirstSubstate().getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(2));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock).deleteAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("0", "Marilyn", "Monroe"));
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("2", "Mother", "Teresa"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(tableDaoMock).findAll();
                verify(tableDaoMock).deleteAll();
                List<Table> tablesExpectedToBeingStoredNow = new LinkedList<>();
                tablesExpectedToBeingStoredNow.add(new Table(0, false));
                tablesExpectedToBeingStoredNow.add(new Table(1, true));
                tablesExpectedToBeingStoredNow.add(new Table(2, true));
                tablesExpectedToBeingStoredNow.add(new Table(3, false));
                tablesExpectedToBeingStoredNow.add(new Table(4, true));
                verify(tableDaoMock).insertAll(tablesExpectedToBeingStoredNow);
            }
        });
    }

    @Test
    public void makeReservations_noLocalTables_remoteTables_chooseRemoteUnavailableTable_shouldYieldThatTableNonetheless() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four customers below.
        String remoteCustomers = "[\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Marilyn\",\n" +
            "    \"customerLastName\": \"Monroe\",\n" +
            "    \"id\": 0\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Abraham\",\n" +
            "    \"customerLastName\": \"Lincoln\",\n" +
            "    \"id\": 1\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Mother\",\n" +
            "    \"customerLastName\": \"Teresa\",\n" +
            "    \"id\": 2\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"John F.\",\n" +
            "    \"customerLastName\": \"Kennedy\",\n" +
            "    \"id\": 3\n" +
            "  }\n" +
            "]";
        //Setting up database to return these two customers below;
        List<Customer> localCustomers = new LinkedList<>();
        localCustomers.add(new Customer("2", "Mother", "Teresa"));
        localCustomers.add(new Customer("0", "Marilyn", "Monroe"));

        //Setting up mock server to return these five tables below.
        String remoteTables = "[false, true, true, false, true]";
        //Setting up database to return this empty list of tables below.
        List<Table> localTables = new LinkedList<>();

        setupMakeReservationsTest(localCustomers, remoteCustomers, localTables, remoteTables, new MakeReservationsTestSetupCallback() {
            @Override
            public void onSetupComplete(MockWebServer server,
                                        CustomerDao customerDaoMock,
                                        TableDao tableDaoMock,
                                        MakeReservationsUseCase makeReservationsUseCase,
                                        FetchCustomersViewState.SuccessFetchingCustomers localSuccessFetchingCustomers,
                                        FetchCustomersViewState.SuccessFetchingCustomers remoteSuccessFetchingCustomers) throws Exception {

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(localSuccessFetchingCustomers.getCustomers(), hasSize(2));
                assertThat(localSuccessFetchingCustomers.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                // ### PREREQUISITES EXECUTION PHASE ###

                Customer motherTheresa = localSuccessFetchingCustomers.getCustomers().get(0);
                ChooseCustomerAction prerequisiteAction = new ChooseCustomerAction(localSuccessFetchingCustomers, motherTheresa);

                final List<MakeReservationsViewState> states = new LinkedList<>();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseCustomer(prerequisiteAction).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen loading = (MakeReservationsViewState.CustomerChosen) states.get(0);
                assertThat(loading.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(loading.getSecondSubstate(), instanceOf(FetchTablesViewState.FetchingTables.class));

                assertThat(states.get(1), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen localSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(1);
                assertThat(localSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(localSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables localSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) localSuccessFetchingTables.getSecondSubstate();
                assertThat(localSuccessSubstate.getTables(), hasSize(0));

                assertThat(states.get(2), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen remoteSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(2);
                assertThat(remoteSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables remoteSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) remoteSuccessFetchingTables.getSecondSubstate();
                assertThat(remoteSuccessSubstate.getTables(), hasSize(5));
                assertThat(remoteSuccessSubstate.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccessSubstate.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));

                // ### EXECUTION PHASE ###

                Table tableNumberThree = remoteSuccessSubstate.getTables().get(3);
                ChooseTableAction action = new ChooseTableAction
                    (remoteSuccessFetchingTables, remoteSuccessSubstate, tableNumberThree);

                states.clear();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseTable(action).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(1));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.TableChosen.class));

                MakeReservationsViewState.TableChosen tableChosen
                    = (MakeReservationsViewState.TableChosen) states.get(0);
                assertThat(tableChosen.getChosenTable(), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(tableChosen.getSecondSubstate().getTables(), hasSize(5));
                assertThat(tableChosen.getSecondSubstate().getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(tableChosen.getFirstSubstate().getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(2));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock).deleteAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("0", "Marilyn", "Monroe"));
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("2", "Mother", "Teresa"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(tableDaoMock).findAll();
                verify(tableDaoMock).deleteAll();
                List<Table> tablesExpectedToBeingStoredNow = new LinkedList<>();
                tablesExpectedToBeingStoredNow.add(new Table(0, false));
                tablesExpectedToBeingStoredNow.add(new Table(1, true));
                tablesExpectedToBeingStoredNow.add(new Table(2, true));
                tablesExpectedToBeingStoredNow.add(new Table(3, false));
                tablesExpectedToBeingStoredNow.add(new Table(4, true));
                verify(tableDaoMock).insertAll(tablesExpectedToBeingStoredNow);
            }
        });
    }

    @Test
    public void makeReservations_localTables_errorFetchingRemoteTables_chooseLocalAvailableTable_shouldYieldThatTableNonetheless() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four customers below.
        String remoteCustomers = "[\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Marilyn\",\n" +
            "    \"customerLastName\": \"Monroe\",\n" +
            "    \"id\": 0\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Abraham\",\n" +
            "    \"customerLastName\": \"Lincoln\",\n" +
            "    \"id\": 1\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"Mother\",\n" +
            "    \"customerLastName\": \"Teresa\",\n" +
            "    \"id\": 2\n" +
            "  },\n" +
            "  {\n" +
            "    \"customerFirstName\": \"John F.\",\n" +
            "    \"customerLastName\": \"Kennedy\",\n" +
            "    \"id\": 3\n" +
            "  }\n" +
            "]";
        //Setting up database to return these two customers below;
        List<Customer> localCustomers = new LinkedList<>();
        localCustomers.add(new Customer("2", "Mother", "Teresa"));
        localCustomers.add(new Customer("0", "Marilyn", "Monroe"));

        //Setting up mock server to return these five tables below.
        String remoteTables = null;
        //Setting up database to return this empty list of tables below.
        List<Table> localTables = new LinkedList<>();
        localTables.add(new Table(0, false));
        localTables.add(new Table(1, true));
        localTables.add(new Table(2, true));
        localTables.add(new Table(3, false));
        localTables.add(new Table(4, true));

        setupMakeReservationsTest(localCustomers, remoteCustomers, localTables, remoteTables, new MakeReservationsTestSetupCallback() {
            @Override
            public void onSetupComplete(MockWebServer server,
                                        CustomerDao customerDaoMock,
                                        TableDao tableDaoMock,
                                        MakeReservationsUseCase makeReservationsUseCase,
                                        FetchCustomersViewState.SuccessFetchingCustomers localSuccessFetchingCustomers,
                                        FetchCustomersViewState.SuccessFetchingCustomers remoteSuccessFetchingCustomers) throws Exception {

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(localSuccessFetchingCustomers.getCustomers(), hasSize(2));
                assertThat(localSuccessFetchingCustomers.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                // ### PREREQUISITES EXECUTION PHASE ###

                Customer motherTheresa = localSuccessFetchingCustomers.getCustomers().get(0);
                ChooseCustomerAction prerequisiteAction = new ChooseCustomerAction(localSuccessFetchingCustomers, motherTheresa);

                final List<MakeReservationsViewState> states = new LinkedList<>();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseCustomer(prerequisiteAction).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### PREREQUISITES VERIFICATION PHASE ###

                assertThat(states, hasSize(4));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen loading = (MakeReservationsViewState.CustomerChosen) states.get(0);
                assertThat(loading.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(loading.getSecondSubstate(), instanceOf(FetchTablesViewState.FetchingTables.class));

                assertThat(states.get(1), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen localSuccessFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(1);
                assertThat(localSuccessFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(localSuccessFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                FetchTablesViewState.SuccessFetchingTables localSuccessSubstate = (FetchTablesViewState
                    .SuccessFetchingTables) localSuccessFetchingTables.getSecondSubstate();
                assertThat(localSuccessSubstate.getTables(), hasSize(5));
                assertThat(localSuccessSubstate.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccessSubstate.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccessSubstate.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccessSubstate.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccessSubstate.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(states.get(2), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen remoteErrorFetchingTables
                    = (MakeReservationsViewState.CustomerChosen) states.get(2);
                assertThat(remoteErrorFetchingTables.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteErrorFetchingTables.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.ErrorFetchingTables.class));
                FetchTablesViewState.ErrorFetchingTables remoteErrorSubstate = (FetchTablesViewState
                    .ErrorFetchingTables) remoteErrorFetchingTables.getSecondSubstate();
                assertThat(remoteErrorSubstate.getCachedTables(), hasSize(5));
                assertThat(remoteErrorSubstate.getCachedTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteErrorSubstate.getCachedTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteErrorSubstate.getCachedTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteErrorSubstate.getCachedTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteErrorSubstate.getCachedTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(states.get(3), instanceOf(MakeReservationsViewState.CustomerChosen.class));
                MakeReservationsViewState.CustomerChosen initialShownLater
                    = (MakeReservationsViewState.CustomerChosen) states.get(3);
                assertThat(initialShownLater.getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(initialShownLater.getSecondSubstate(),
                    instanceOf(FetchTablesViewState.Initial.class));
                FetchTablesViewState.Initial initialShownLaterSubstate
                    = (FetchTablesViewState.Initial) initialShownLater.getSecondSubstate();
                assertThat(initialShownLaterSubstate.getCachedTables(), hasSize(5));
                assertThat(initialShownLaterSubstate.getCachedTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(initialShownLaterSubstate.getCachedTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(initialShownLaterSubstate.getCachedTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(initialShownLaterSubstate.getCachedTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(initialShownLaterSubstate.getCachedTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));

                // ### EXECUTION PHASE ###

                Table tableNumberFour = localSuccessSubstate.getTables().get(4);
                ChooseTableAction action = new ChooseTableAction
                    (localSuccessFetchingTables, localSuccessSubstate, tableNumberFour);

                states.clear();
                new IterableUtils<MakeReservationsViewState>()
                    .forEach(makeReservationsUseCase.chooseTable(action).blockingIterable(),
                        new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                            @Override
                            public void doForEach(MakeReservationsViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(1));
                assertThat(states.get(0), instanceOf(MakeReservationsViewState.TableChosen.class));

                MakeReservationsViewState.TableChosen tableChosen
                    = (MakeReservationsViewState.TableChosen) states.get(0);
                assertThat(tableChosen.getChosenTable(), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(tableChosen.getSecondSubstate().getTables(), hasSize(5));
                assertThat(tableChosen.getSecondSubstate().getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(tableChosen.getFirstSubstate().getChosenCustomer(), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(2));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock).deleteAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("0", "Marilyn", "Monroe"));
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("2", "Mother", "Teresa"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(tableDaoMock).findAll();
                verify(tableDaoMock, times(0)).deleteAll();
                verify(tableDaoMock, times(0))
                    .insertAll(ArgumentMatchers.<Table>anyList());
            }
        });
    }

    private void setupMakeReservationsTest(final List<Customer> localCustomers, final String remoteCustomers,
                                           final MakeReservationsTestSetupCallback callback) throws Exception {
        setupMakeReservationsTest(localCustomers, remoteCustomers, new LinkedList<Table>(), "[]", callback);
    }

    private void setupMakeReservationsTest(final List<Customer> localCustomers, final String remoteCustomers,
                                           final List<Table> localTables, final String remoteTables,
                                           final MakeReservationsTestSetupCallback callback) throws Exception {
        List<MockResponse> expectedResponses = new LinkedList<>();
        expectedResponses.add(isEmpty(remoteCustomers) ? new MockResponse().setResponseCode(500)
            : new MockResponse().setResponseCode(200).setBody(remoteCustomers));
        expectedResponses.add(isEmpty(remoteTables) ? new MockResponse().setResponseCode(500)
            : new MockResponse().setResponseCode(200).setBody(remoteTables));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                MakeReservationsUseCase makeReservationsUseCase
                    = new MakeReservationsUseCase(baseUrl, mock(Context.class));
                makeReservationsUseCase.setBeingTested();

                CustomerDao customerDaoMock = mock(CustomerDao.class);
                when(customerDaoMock.findAll()).thenReturn(Single.just(localCustomers));

                TableDao tableDaoMock = mock(TableDao.class);
                when(tableDaoMock.findAll()).thenReturn(Single.just(localTables));

                //Turning database writes into no-ops.
                doNothing().when(customerDaoMock).deleteAll();
                doNothing().when(customerDaoMock).insertAll(ArgumentMatchers.<Customer>anyList());
                doNothing().when(tableDaoMock).deleteAll();
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.customerDao()).thenReturn(customerDaoMock);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                FetchCustomersUseCase fetchCustomersUseCase = spy(makeReservationsUseCase.getFetchCustomersUseCase());
                makeReservationsUseCase.setFetchCustomersUseCase(fetchCustomersUseCase);
                doReturn(databaseMock).when(fetchCustomersUseCase).getDatabase();
                FetchTablesUseCase fetchTablesUseCase = spy(makeReservationsUseCase.getFetchTablesUseCase());
                makeReservationsUseCase.setFetchTablesUseCase(fetchTablesUseCase);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();
                makeReservationsUseCase = spy(makeReservationsUseCase);
                doReturn(databaseMock).when(makeReservationsUseCase).getDatabase();

                final List<MakeReservationsViewState> states = new LinkedList<>();
                    new IterableUtils<MakeReservationsViewState>()
                        .forEach(makeReservationsUseCase.fetchCustomers(new FetchCustomersAction()).blockingIterable(),
                            new IterableUtils.IterableCallback<MakeReservationsViewState>() {
                                @Override
                                public void doForEach(MakeReservationsViewState state) {
                                    states.add(state);
                                }
                            }
                        );

                assertThat(states, hasSize(3));

                assertThat(states.get(0), instanceOf(MakeReservationsViewState.Initial.class));
                MakeReservationsViewState.Initial loading = (MakeReservationsViewState.Initial) states.get(0);
                assertThat(loading.getSubstate(), instanceOf(FetchCustomersViewState.FetchingCustomers.class));

                assertThat(states.get(1), instanceOf(MakeReservationsViewState.Initial.class));
                MakeReservationsViewState.Initial localSuccess = (MakeReservationsViewState.Initial) states.get(1);
                assertThat(localSuccess.getSubstate(), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));

                assertThat(states.get(2), instanceOf(MakeReservationsViewState.Initial.class));
                MakeReservationsViewState.Initial remoteSuccess = (MakeReservationsViewState.Initial) states.get(2);
                assertThat(remoteSuccess.getSubstate(), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));

                callback.onSetupComplete(server, customerDaoMock, tableDaoMock, makeReservationsUseCase,
                    (FetchCustomersViewState.SuccessFetchingCustomers) localSuccess.getSubstate(),
                    (FetchCustomersViewState.SuccessFetchingCustomers) remoteSuccess.getSubstate());
            }
        });
    }

    interface MakeReservationsTestSetupCallback {

        void onSetupComplete(MockWebServer server,
                             CustomerDao customerDaoMock,
                             TableDao tableDaoMock,
                             MakeReservationsUseCase makeReservationsUseCase,
                             FetchCustomersViewState.SuccessFetchingCustomers localSuccess,
                             FetchCustomersViewState.SuccessFetchingCustomers remoteSuccess) throws Exception;

    }

}
