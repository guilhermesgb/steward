package com.github.guilhermesgb.steward.utils;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.network.ApiEndpoints;
import com.github.guilhermesgb.steward.network.ApiResource;

public abstract class UseCase {

    private final String apiBaseUrl; //for networking purposes (endpoint calls)
    private final Context context;  //for persistence purposes (database operations)

    public UseCase(String apiBaseUrl, Context context) {
        this.apiBaseUrl = apiBaseUrl;
        this.context = context;
    }

    public ApiEndpoints getApi() {
        return ApiResource.getInstance(apiBaseUrl);
    }

    public DatabaseResource getDatabase() {
        return DatabaseResource.getInstance(context);
    }

}
