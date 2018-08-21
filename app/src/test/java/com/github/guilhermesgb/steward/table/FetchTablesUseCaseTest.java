package com.github.guilhermesgb.steward.table;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.mvi.reservation.schema.ReservationDao;
import com.github.guilhermesgb.steward.mvi.table.FetchTablesUseCase;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.schema.TableDao;
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

public class FetchTablesUseCaseTest extends MockedServerUnitTest {

    @Test
    public void fetchTables_noLocalTables_noRemoteTables_shouldYieldEmptyResults() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return empty list of tables.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                TableDao tableDaoMock = mock(TableDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return empty list of tables as well.
                when(tableDaoMock.findAll()).thenReturn
                    (Single.<List<Table>>just(new LinkedList<Table>()));
                //Turning database writes into no-ops.
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedTables();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchTablesViewState> states = new LinkedList<>();
                new IterableUtils<FetchTablesViewState>()
                    .forEach(fetchTablesUseCase.doFetchTables(new FetchTablesAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchTablesViewState>() {
                            @Override
                            public void doForEach(FetchTablesViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.SuccessFetchingTables remoteSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(2);

                assertThat(localSuccess.getTables(), hasSize(0));

                assertThat(remoteSuccess.getTables(), hasSize(0));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(tableDaoMock).findAll();
                verify(tableDaoMock).insertAll(new LinkedList<Table>());
                verify(reservationDaoMock).deleteUnusedTables();
            }
        });
    }

    @Test
    public void fetchTables_noLocalTables_someRemoteTables_shouldYieldTheseRemoteTables() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four tables below.
        List<MockResponse> expectedResponses = Collections.singletonList
                (new MockResponse().setResponseCode(200).setBody("[true, false, false, true]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                TableDao tableDaoMock = mock(TableDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return empty list of tables.
                when(tableDaoMock.findAll()).thenReturn
                    (Single.<List<Table>>just(new LinkedList<Table>()));
                //Turning database writes into no-ops.
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedTables();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchTablesViewState> states = new LinkedList<>();
                new IterableUtils<FetchTablesViewState>()
                    .forEach(fetchTablesUseCase.doFetchTables(new FetchTablesAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchTablesViewState>() {
                            @Override
                            public void doForEach(FetchTablesViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.SuccessFetchingTables remoteSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(2);

                assertThat(localSuccess.getTables(), hasSize(0));

                assertThat(remoteSuccess.getTables(), hasSize(4));
                assertThat(remoteSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(tableDaoMock).findAll();
                List<Table> tablesExpectedToBeingStoredNow = new LinkedList<>();
                tablesExpectedToBeingStoredNow.add(new Table(0, true));
                tablesExpectedToBeingStoredNow.add(new Table(1, false));
                tablesExpectedToBeingStoredNow.add(new Table(2, false));
                tablesExpectedToBeingStoredNow.add(new Table(3, true));
                verify(tableDaoMock).insertAll(tablesExpectedToBeingStoredNow);
                verify(reservationDaoMock).deleteUnusedTables();
            }
        });
    }

    @Test
    public void fetchTables_someLocalTables_sameAmountOfRemoteTables_shouldPreserveLocalTables() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these four tables below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[true, false, false, true]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                TableDao tableDaoMock = mock(TableDao.class);
                //Mocking database to return these four previously stored tables.
                List<Table> tablesExpectedToHaveBeenStoredThen = new LinkedList<>();
                tablesExpectedToHaveBeenStoredThen.add(new Table(0, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(1, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(2, true));
                tablesExpectedToHaveBeenStoredThen.add(new Table(3, true));
                when(tableDaoMock.findAll()).thenReturn
                    (Single.just(tablesExpectedToHaveBeenStoredThen));
                //Turning database writes into no-ops.
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedTables();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchTablesViewState> states = new LinkedList<>();
                new IterableUtils<FetchTablesViewState>()
                    .forEach(fetchTablesUseCase.doFetchTables(new FetchTablesAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchTablesViewState>() {
                            @Override
                            public void doForEach(FetchTablesViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.SuccessFetchingTables remoteSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(2);

                assertThat(localSuccess.getTables(), hasSize(4));
                assertThat(localSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(remoteSuccess.getTables(), hasSize(4));
                assertThat(remoteSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(tableDaoMock).findAll();
                List<Table> tablesExpectedToBeingStoredNow = new LinkedList<>();
                tablesExpectedToBeingStoredNow.add(new Table(0, false));
                tablesExpectedToBeingStoredNow.add(new Table(1, false));
                tablesExpectedToBeingStoredNow.add(new Table(2, true));
                tablesExpectedToBeingStoredNow.add(new Table(3, true));
                verify(tableDaoMock).insertAll(tablesExpectedToBeingStoredNow);
                verify(reservationDaoMock).deleteUnusedTables();
            }
        });
    }

    @Test
    public void fetchTables_someLocalTables_moreRemoteTablesAreReturned_shouldPreserveLocalTablesButIncludeNewTablesToo() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these six tables below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[true, false, false, true, false, true]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                TableDao tableDaoMock = mock(TableDao.class);
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                //Mocking database to return these four previously stored tables.
                List<Table> tablesExpectedToHaveBeenStoredThen = new LinkedList<>();
                tablesExpectedToHaveBeenStoredThen.add(new Table(0, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(1, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(2, true));
                tablesExpectedToHaveBeenStoredThen.add(new Table(3, true));
                when(tableDaoMock.findAll()).thenReturn
                    (Single.just(tablesExpectedToHaveBeenStoredThen));
                //Turning database writes into no-ops.
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedTables();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchTablesViewState> states = new LinkedList<>();
                new IterableUtils<FetchTablesViewState>()
                    .forEach(fetchTablesUseCase.doFetchTables(new FetchTablesAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchTablesViewState>() {
                            @Override
                            public void doForEach(FetchTablesViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.SuccessFetchingTables remoteSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(2);

                assertThat(localSuccess.getTables(), hasSize(4));
                assertThat(localSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(remoteSuccess.getTables(), hasSize(6));
                assertThat(remoteSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccess.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(5), allOf(isA(Table.class),
                    hasProperty("number", equalTo(5)),
                    hasProperty("available", equalTo(true))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(tableDaoMock).findAll();
                List<Table> tablesExpectedToBeingStoredNow = new LinkedList<>();
                tablesExpectedToBeingStoredNow.add(new Table(0, false));
                tablesExpectedToBeingStoredNow.add(new Table(1, false));
                tablesExpectedToBeingStoredNow.add(new Table(2, true));
                tablesExpectedToBeingStoredNow.add(new Table(3, true));
                tablesExpectedToBeingStoredNow.add(new Table(4, false));
                tablesExpectedToBeingStoredNow.add(new Table(5, true));
                verify(tableDaoMock).insertAll(tablesExpectedToBeingStoredNow);
                verify(reservationDaoMock).deleteUnusedTables();
            }
        });
    }

    @Test
    public void fetchTables_someLocalTables_lessRemoteTablesAreReturned_shouldPreserveLocalTables() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return these two tables below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[true, false]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                TableDao tableDaoMock = mock(TableDao.class);
                //Mocking database to return these six previously stored tables.
                List<Table> tablesExpectedToHaveBeenStoredThen = new LinkedList<>();
                tablesExpectedToHaveBeenStoredThen.add(new Table(0, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(1, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(2, true));
                tablesExpectedToHaveBeenStoredThen.add(new Table(3, true));
                tablesExpectedToHaveBeenStoredThen.add(new Table(4, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(5, true));
                when(tableDaoMock.findAll()).thenReturn
                    (Single.just(tablesExpectedToHaveBeenStoredThen));
                //Turning database writes into no-ops.
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                doNothing().when(reservationDaoMock).deleteUnusedTables();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchTablesViewState> states = new LinkedList<>();
                new IterableUtils<FetchTablesViewState>()
                    .forEach(fetchTablesUseCase.doFetchTables(new FetchTablesAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchTablesViewState>() {
                            @Override
                            public void doForEach(FetchTablesViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(3));
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.SuccessFetchingTables remoteSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(2);

                assertThat(localSuccess.getTables(), hasSize(6));
                assertThat(localSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccess.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(5), allOf(isA(Table.class),
                    hasProperty("number", equalTo(5)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(remoteSuccess.getTables(), hasSize(6));
                assertThat(remoteSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteSuccess.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteSuccess.getTables().get(5), allOf(isA(Table.class),
                    hasProperty("number", equalTo(5)),
                    hasProperty("available", equalTo(true))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(tableDaoMock).findAll();
                List<Table> tablesExpectedToBeingStoredNow = new LinkedList<>();
                tablesExpectedToBeingStoredNow.add(new Table(0, false));
                tablesExpectedToBeingStoredNow.add(new Table(1, false));
                tablesExpectedToBeingStoredNow.add(new Table(2, true));
                tablesExpectedToBeingStoredNow.add(new Table(3, true));
                tablesExpectedToBeingStoredNow.add(new Table(4, false));
                tablesExpectedToBeingStoredNow.add(new Table(5, true));
                verify(tableDaoMock).insertAll(tablesExpectedToBeingStoredNow);
                verify(reservationDaoMock).deleteUnusedTables();
            }
        });
    }

    @Test
    public void fetchTables_someLocalTables_errorFetchingRemoteTables_shouldPreserveLocalTables() throws Exception {
        // ### SETUP PHASE ###

        //Setting up mock server to return the error below.
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(500));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                TableDao tableDaoMock = mock(TableDao.class);
                //Mocking database to return these six previously stored tables.
                List<Table> tablesExpectedToHaveBeenStoredThen = new LinkedList<>();
                tablesExpectedToHaveBeenStoredThen.add(new Table(0, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(1, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(2, true));
                tablesExpectedToHaveBeenStoredThen.add(new Table(3, true));
                tablesExpectedToHaveBeenStoredThen.add(new Table(4, false));
                tablesExpectedToHaveBeenStoredThen.add(new Table(5, true));
                when(tableDaoMock.findAll()).thenReturn
                    (Single.just(tablesExpectedToHaveBeenStoredThen));
                //Turning database writes into no-ops.
                doNothing().when(tableDaoMock).insertAll(ArgumentMatchers.<Table>anyList());
                ReservationDao reservationDaoMock = mock(ReservationDao.class);
                doNothing().when(reservationDaoMock).deleteUnusedTables();
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                when(databaseMock.reservationDao()).thenReturn(reservationDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

                // ### EXECUTION PHASE ###

                final List<FetchTablesViewState> states = new LinkedList<>();
                new IterableUtils<FetchTablesViewState>()
                    .forEach(fetchTablesUseCase.doFetchTables(new FetchTablesAction()).blockingIterable(),
                        new IterableUtils.IterableCallback<FetchTablesViewState>() {
                            @Override
                            public void doForEach(FetchTablesViewState state) {
                                states.add(state);
                            }
                        }
                    );

                // ### VERIFICATION PHASE ###

                assertThat(states, hasSize(4));
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.ErrorFetchingTables.class));
                assertThat(states.get(3), instanceOf(FetchTablesViewState.Initial.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.ErrorFetchingTables remoteError
                    = (FetchTablesViewState.ErrorFetchingTables) states.get(2);
                FetchTablesViewState.Initial initialShownLater
                    = (FetchTablesViewState.Initial) states.get(3);

                assertThat(localSuccess.getTables(), hasSize(6));
                assertThat(localSuccess.getTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccess.getTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(localSuccess.getTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(localSuccess.getTables().get(5), allOf(isA(Table.class),
                    hasProperty("number", equalTo(5)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(remoteError.getCachedTables(), hasSize(6));
                assertThat(remoteError.getCachedTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteError.getCachedTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteError.getCachedTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteError.getCachedTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(remoteError.getCachedTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(remoteError.getCachedTables().get(5), allOf(isA(Table.class),
                    hasProperty("number", equalTo(5)),
                    hasProperty("available", equalTo(true))
                ));

                assertThat(initialShownLater.getCachedTables(), hasSize(6));
                assertThat(initialShownLater.getCachedTables().get(0), allOf(isA(Table.class),
                    hasProperty("number", equalTo(0)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(initialShownLater.getCachedTables().get(1), allOf(isA(Table.class),
                    hasProperty("number", equalTo(1)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(initialShownLater.getCachedTables().get(2), allOf(isA(Table.class),
                    hasProperty("number", equalTo(2)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(initialShownLater.getCachedTables().get(3), allOf(isA(Table.class),
                    hasProperty("number", equalTo(3)),
                    hasProperty("available", equalTo(true))
                ));
                assertThat(initialShownLater.getCachedTables().get(4), allOf(isA(Table.class),
                    hasProperty("number", equalTo(4)),
                    hasProperty("available", equalTo(false))
                ));
                assertThat(initialShownLater.getCachedTables().get(5), allOf(isA(Table.class),
                    hasProperty("number", equalTo(5)),
                    hasProperty("available", equalTo(true))
                ));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();

                //Verifying if test made expected database operations.
                verify(tableDaoMock).findAll();
                verify(tableDaoMock, times(0))
                    .insertAll(ArgumentMatchers.<Table>anyList());
                verify(reservationDaoMock, times(0)).deleteUnusedTables();
            }
        });
    }

    private FetchTablesUseCase fetchTablesUseCase(String baseUrl) {
        FetchTablesUseCase fetchTablesUseCase
            = new FetchTablesUseCase(baseUrl,
                mock(Context.class));
        fetchTablesUseCase.setBeingTested();
        return spy(fetchTablesUseCase);
    }

}
