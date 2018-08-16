package com.github.guilhermesgb.steward.utils;

import java.util.List;

import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public abstract class MockedServerUnitTest {

    protected void configureMockWebServer(List<MockResponse> expectedResponses,
                                          MockServerCallback callback) throws Exception {
        MockWebServer server = new MockWebServer();
        for (MockResponse response : expectedResponses) {
            server.enqueue(response);
        }
        server.start();
        HttpUrl baseUrl = server.url("");
        callback.onMockServerConfigured(server, baseUrl.toString());
    }

    protected interface MockServerCallback {

        void onMockServerConfigured(MockWebServer server, String baseUrl) throws Exception;

    }

}
