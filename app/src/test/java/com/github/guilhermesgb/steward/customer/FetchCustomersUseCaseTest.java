package com.github.guilhermesgb.steward.customer;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.mvi.customer.FetchCustomersUseCase;
import com.github.guilhermesgb.steward.mvi.customer.intent.FetchCustomersAction;
import com.github.guilhermesgb.steward.mvi.customer.model.FetchCustomersViewState;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.customer.schema.CustomerDao;
import com.github.guilhermesgb.steward.mvi.reservation.schema.ReservationDao;
import com.github.guilhermesgb.steward.network.ApiEndpoints;
import com.github.guilhermesgb.steward.utils.IterableUtils;
import com.github.guilhermesgb.steward.utils.MockedServerUnitTest;

import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Single;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.http.GET;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FetchCustomersUseCaseTest extends MockedServerUnitTest {

    @Test
    public void fetchCustomers_noLocalCustomers_noRemoteCustomers_shouldYieldEmptyResults() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return empty list of customers.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchCustomersUseCase fetchCustomersUseCase = fetchCustomersUseCase(baseUrl);

                CustomerDao customerDaoMock = mock(CustomerDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return empty list of customers as well.
                when(customerDaoMock.findAll()).thenReturn
                    (Single.<List<Customer>>just(new LinkedList<Customer>()));
                //Turning database writes into no-ops.
                doNothing().when(customerDaoMock).insertAll(ArgumentMatchers.<Customer>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedCustomers();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.customerDao()).thenReturn(customerDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchCustomersUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchCustomersViewState> states = new LinkedList<>();
                new IterableUtils<FetchCustomersViewState>()
                    .forEach(fetchCustomersUseCase.doFetchCustomers(new FetchCustomersAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchCustomersViewState>() {
                            @Override
                            public void doForEach(FetchCustomersViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchCustomersViewState.FetchingCustomers.class));
                assertThat(states.get(1), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));
                assertThat(states.get(2), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));

                FetchCustomersViewState.SuccessFetchingCustomers localSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(1);
                FetchCustomersViewState.SuccessFetchingCustomers remoteSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(2);

                assertThat(localSuccess.getCustomers(), hasSize(0));

                assertThat(remoteSuccess.getCustomers(), hasSize(0));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock).insertAll(new LinkedList<Customer>());
                verify(reservationDaoMock).deleteUnusedCustomers();
            }
        });
    }

    @Test
    public void fetchCustomers_noLocalCustomers_someRemoteCustomers_shouldYieldTheseRemoteCustomers() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four customers below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[\n" +
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
                "]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchCustomersUseCase fetchCustomersUseCase = fetchCustomersUseCase(baseUrl);

                CustomerDao customerDaoMock = mock(CustomerDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return empty list of customers.
                when(customerDaoMock.findAll()).thenReturn
                    (Single.<List<Customer>>just(new LinkedList<Customer>()));
                //Turning database writes into no-ops.
                doNothing().when(customerDaoMock).insertAll(ArgumentMatchers.<Customer>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedCustomers();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.customerDao()).thenReturn(customerDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchCustomersUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchCustomersViewState> states = new LinkedList<>();
                new IterableUtils<FetchCustomersViewState>()
                    .forEach(fetchCustomersUseCase.doFetchCustomers(new FetchCustomersAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchCustomersViewState>() {
                            @Override
                            public void doForEach(FetchCustomersViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchCustomersViewState.FetchingCustomers.class));
                assertThat(states.get(1), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));
                assertThat(states.get(2), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));

                FetchCustomersViewState.SuccessFetchingCustomers localSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(1);
                FetchCustomersViewState.SuccessFetchingCustomers remoteSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(2);

                assertThat(localSuccess.getCustomers(), hasSize(0));

                assertThat(remoteSuccess.getCustomers(), hasSize(4));
                assertThat(remoteSuccess.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("0")),
                    hasProperty("firstName", equalTo("Marilyn")),
                    hasProperty("lastName", equalTo("Monroe"))
                ));
                assertThat(remoteSuccess.getCustomers().get(1), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("1")),
                    hasProperty("firstName", equalTo("Abraham")),
                    hasProperty("lastName", equalTo("Lincoln"))
                ));
                assertThat(remoteSuccess.getCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteSuccess.getCustomers().get(3), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("3")),
                    hasProperty("firstName", equalTo("John F.")),
                    hasProperty("lastName", equalTo("Kennedy"))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("0", "Marilyn", "Monroe"));
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("2", "Mother", "Teresa"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(reservationDaoMock).deleteUnusedCustomers();
            }
        });
    }

    @Test
    public void fetchCustomers_someLocalCustomers_someRemoteCustomers_shouldOverrideWithRemoteCustomers() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four customers below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[\n" +
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
                "]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchCustomersUseCase fetchCustomersUseCase = fetchCustomersUseCase(baseUrl);

                CustomerDao customerDaoMock = mock(CustomerDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return these three previously stored customers.
                List<Customer> customersExpectedToHaveBeenStoredThen = new LinkedList<>();
                customersExpectedToHaveBeenStoredThen.add(new Customer("0", "Marilyn", "The Woman"));
                customersExpectedToHaveBeenStoredThen.add(new Customer("4", "Martin Luther", "King"));
                customersExpectedToHaveBeenStoredThen.add(new Customer("5", "Nelson", "Mandela"));
                when(customerDaoMock.findAll()).thenReturn
                    (Single.just(customersExpectedToHaveBeenStoredThen));
                //Turning database writes into no-ops.
                doNothing().when(customerDaoMock).insertAll(ArgumentMatchers.<Customer>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedCustomers();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.customerDao()).thenReturn(customerDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchCustomersUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchCustomersViewState> states = new LinkedList<>();
                new IterableUtils<FetchCustomersViewState>()
                    .forEach(fetchCustomersUseCase.doFetchCustomers(new FetchCustomersAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchCustomersViewState>() {
                            @Override
                            public void doForEach(FetchCustomersViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchCustomersViewState.FetchingCustomers.class));
                assertThat(states.get(1), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));
                assertThat(states.get(2), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));

                FetchCustomersViewState.SuccessFetchingCustomers localSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(1);
                FetchCustomersViewState.SuccessFetchingCustomers remoteSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(2);

                assertThat(localSuccess.getCustomers(), hasSize(3));
                assertThat(localSuccess.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("0")),
                    hasProperty("firstName", equalTo("Marilyn")),
                    hasProperty("lastName", equalTo("The Woman"))
                ));
                assertThat(localSuccess.getCustomers().get(1), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("4")),
                    hasProperty("firstName", equalTo("Martin Luther")),
                    hasProperty("lastName", equalTo("King"))
                ));
                assertThat(localSuccess.getCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("5")),
                    hasProperty("firstName", equalTo("Nelson")),
                    hasProperty("lastName", equalTo("Mandela"))
                ));

                assertThat(remoteSuccess.getCustomers(), hasSize(4));
                assertThat(remoteSuccess.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("0")),
                    hasProperty("firstName", equalTo("Marilyn")),
                    hasProperty("lastName", equalTo("Monroe"))
                ));
                assertThat(remoteSuccess.getCustomers().get(1), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("1")),
                    hasProperty("firstName", equalTo("Abraham")),
                    hasProperty("lastName", equalTo("Lincoln"))
                ));
                assertThat(remoteSuccess.getCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("2")),
                    hasProperty("firstName", equalTo("Mother")),
                    hasProperty("lastName", equalTo("Teresa"))
                ));
                assertThat(remoteSuccess.getCustomers().get(3), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("3")),
                    hasProperty("firstName", equalTo("John F.")),
                    hasProperty("lastName", equalTo("Kennedy"))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                List<Customer> customersExpectedToBeingStoredNow = new LinkedList<>();
                customersExpectedToBeingStoredNow.add(new Customer("0", "Marilyn", "Monroe"));
                customersExpectedToBeingStoredNow.add(new Customer("1", "Abraham", "Lincoln"));
                customersExpectedToBeingStoredNow.add(new Customer("2", "Mother", "Teresa"));
                customersExpectedToBeingStoredNow.add(new Customer("3", "John F.", "Kennedy"));
                verify(customerDaoMock).insertAll(customersExpectedToBeingStoredNow);
                verify(reservationDaoMock).deleteUnusedCustomers();
            }
        });
    }

    @Test
    public void fetchCustomers_someLocalCustomers_errorFetchingRemoteCustomers_shouldPreserveLocalCustomers() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return the error below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(500));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchCustomersUseCase fetchCustomersUseCase = fetchCustomersUseCase(baseUrl);

                CustomerDao customerDaoMock = mock(CustomerDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return these three previously stored customers.
                List<Customer> customersExpectedToHaveBeenStoredThen = new LinkedList<>();
                customersExpectedToHaveBeenStoredThen.add(new Customer("0", "Marilyn", "The Woman"));
                customersExpectedToHaveBeenStoredThen.add(new Customer("4", "Martin Luther", "King"));
                customersExpectedToHaveBeenStoredThen.add(new Customer("5", "Nelson", "Mandela"));
                when(customerDaoMock.findAll()).thenReturn
                    (Single.just(customersExpectedToHaveBeenStoredThen));
                //Turning database writes into no-ops.
                doNothing().when(reservationDaoMock).deleteUnusedCustomers();
                doNothing().when(customerDaoMock).insertAll(ArgumentMatchers.<Customer>anyList());
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.customerDao()).thenReturn(customerDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchCustomersUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchCustomersViewState> states = new LinkedList<>();
                new IterableUtils<FetchCustomersViewState>()
                    .forEach(fetchCustomersUseCase.doFetchCustomers(new FetchCustomersAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchCustomersViewState>() {
                            @Override
                            public void doForEach(FetchCustomersViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(4));
                assertThat(states.get(0), instanceOf(FetchCustomersViewState.FetchingCustomers.class));
                assertThat(states.get(1), instanceOf(FetchCustomersViewState.SuccessFetchingCustomers.class));
                assertThat(states.get(2), instanceOf(FetchCustomersViewState.ErrorFetchingCustomers.class));
                assertThat(states.get(3), instanceOf(FetchCustomersViewState.Initial.class));

                FetchCustomersViewState.SuccessFetchingCustomers localSuccess
                    = (FetchCustomersViewState.SuccessFetchingCustomers) states.get(1);
                FetchCustomersViewState.ErrorFetchingCustomers remoteError
                    = (FetchCustomersViewState.ErrorFetchingCustomers) states.get(2);
                FetchCustomersViewState.Initial initialShownLater
                    = (FetchCustomersViewState.Initial) states.get(3);

                assertThat(localSuccess.getCustomers(), hasSize(3));
                assertThat(localSuccess.getCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("0")),
                    hasProperty("firstName", equalTo("Marilyn")),
                    hasProperty("lastName", equalTo("The Woman"))
                ));
                assertThat(localSuccess.getCustomers().get(1), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("4")),
                    hasProperty("firstName", equalTo("Martin Luther")),
                    hasProperty("lastName", equalTo("King"))
                ));
                assertThat(localSuccess.getCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("5")),
                    hasProperty("firstName", equalTo("Nelson")),
                    hasProperty("lastName", equalTo("Mandela"))
                ));

                assertThat(remoteError.getCachedCustomers(), hasSize(3));
                assertThat(remoteError.getCachedCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("0")),
                    hasProperty("firstName", equalTo("Marilyn")),
                    hasProperty("lastName", equalTo("The Woman"))
                ));
                assertThat(remoteError.getCachedCustomers().get(1), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("4")),
                    hasProperty("firstName", equalTo("Martin Luther")),
                    hasProperty("lastName", equalTo("King"))
                ));
                assertThat(remoteError.getCachedCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("5")),
                    hasProperty("firstName", equalTo("Nelson")),
                    hasProperty("lastName", equalTo("Mandela"))
                ));

                assertThat(initialShownLater.getCachedCustomers(), hasSize(3));
                assertThat(initialShownLater.getCachedCustomers().get(0), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("0")),
                    hasProperty("firstName", equalTo("Marilyn")),
                    hasProperty("lastName", equalTo("The Woman"))
                ));
                assertThat(initialShownLater.getCachedCustomers().get(1), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("4")),
                    hasProperty("firstName", equalTo("Martin Luther")),
                    hasProperty("lastName", equalTo("King"))
                ));
                assertThat(initialShownLater.getCachedCustomers().get(2), allOf(isA(Customer.class),
                    hasProperty("id", equalTo("5")),
                    hasProperty("firstName", equalTo("Nelson")),
                    hasProperty("lastName", equalTo("Mandela"))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchCustomers").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(customerDaoMock).findAll();
                verify(customerDaoMock, times(0))
                    .insertAll(ArgumentMatchers.<Customer>anyList());
                verify(reservationDaoMock, times(0))
                    .deleteUnusedCustomers();
            }
        });
    }

    private FetchCustomersUseCase fetchCustomersUseCase(String baseUrl) {
        FetchCustomersUseCase fetchCustomersUseCase
            = new FetchCustomersUseCase(baseUrl,
                mock(Context.class));
        fetchCustomersUseCase.setBeingTested();
        return spy(fetchCustomersUseCase);
    }

}
