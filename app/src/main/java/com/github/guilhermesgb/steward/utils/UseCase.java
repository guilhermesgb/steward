package com.github.guilhermesgb.steward.utils;

import android.content.Context;

import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.network.ApiEndpoints;
import com.github.guilhermesgb.steward.network.ApiResource;

public abstract class UseCase {

    private final String apiBaseUrl; //for networking purposes (endpoint calls)
    private final Context context;  //for persistence purposes (database operations)
    //Flag to indicate whether this use case is being tested, intended so that the
    // use case may shorten time until delayed emission of some states (which are
    // delayed on purpose e.g. the initial state after 5 seconds of the error state
    // effectively making the error state only last 5 seconds at most).
    //This is done because we don't want to wait these 5 seconds when running our unit tests.
    private boolean beingTested = false;

    public UseCase(String apiBaseUrl, Context context) {
        this.apiBaseUrl = apiBaseUrl;
        this.context = context;
    }

    protected ApiEndpoints getApi() {
        return ApiResource.getInstance(apiBaseUrl);
    }

    public DatabaseResource getDatabase() {
        return DatabaseResource.getInstance(context);
    }

    protected boolean isBeingTested() {
        return beingTested;
    }

    public void setBeingTested() {
        beingTested = true;
    }

}
