package com.github.guilhermesgb.steward.table;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.mvi.table.FetchTablesUseCase;
import com.github.guilhermesgb.steward.mvi.table.intent.FetchTablesAction;
import com.github.guilhermesgb.steward.mvi.table.model.FetchTablesViewState;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.schema.TableDao;
import com.github.guilhermesgb.steward.network.ApiEndpoints;
import com.github.guilhermesgb.steward.utils.IterableUtils;
import com.github.guilhermesgb.steward.utils.MockedServerUnitTest;

import org.junit.Test;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import io.reactivex.Single;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import retrofit2.http.GET;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyListOf;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@SuppressWarnings("deprecation")
public class FetchTablesUseCaseTest extends MockedServerUnitTest {

    @Test
    public void fetchTables_noLocalTables_noRemoteTables_shouldYieldEmptyResults() throws Exception {
        List<MockResponse> expectedResponses = Collections.singletonList
            (new MockResponse().setResponseCode(200).setBody("[]"));
        configureMockWebServer(expectedResponses, new MockServerCallback() {
            @Override
            public void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception {
                FetchTablesUseCase fetchTablesUseCase = fetchTablesUseCase(baseUrl);

                //Mocking database to do effectively turn inserts into the database into no-ops.
                TableDao tableDaoMock = mock(TableDao.class);
                when(tableDaoMock.findAll()).thenReturn
                    (Single.<List<Table>>just(new LinkedList<Table>()));
                doNothing().when(tableDaoMock).deleteAll();
                doNothing().when(tableDaoMock).insertAll(anyListOf(Table.class));
                DatabaseResource databaseMock = mock(DatabaseResource.class);
                when(databaseMock.tableDao()).thenReturn(tableDaoMock);
                doReturn(databaseMock).when(fetchTablesUseCase).getDatabase();

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

                assertThat(states, hasSize(3)); //loading, success[local], success[remote]
                assertThat(states.get(0), instanceOf(FetchTablesViewState.FetchingTables.class));
                assertThat(states.get(1), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));
                assertThat(states.get(2), instanceOf(FetchTablesViewState.SuccessFetchingTables.class));

                FetchTablesViewState.SuccessFetchingTables localSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(1);
                FetchTablesViewState.SuccessFetchingTables remoteSuccess
                    = (FetchTablesViewState.SuccessFetchingTables) states.get(2);
                //noinspection unchecked
                assertThat(localSuccess.getTables(), hasSize(0));
                assertThat(remoteSuccess.getTables(), hasSize(0));

                //Verifying if test made expected API calls.
                assertThat(server.getRequestCount(), is(1));
                String expectedEndpoint = "/" + ApiEndpoints.class
                    .getMethod("fetchTables").getAnnotation(GET.class).value();
                RecordedRequest request = server.takeRequest();
                assertThat(request.getPath(), is(expectedEndpoint));
                server.shutdown();
            }
        });
    }

    private FetchTablesUseCase fetchTablesUseCase(String baseUrl) {
        return spy(new FetchTablesUseCase(baseUrl, mock(Context.class)));
    }

}
