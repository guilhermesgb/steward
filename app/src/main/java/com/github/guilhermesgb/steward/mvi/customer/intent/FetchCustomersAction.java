package com.github.guilhermesgb.steward.mvi.customer.intent;

public class FetchCustomersAction {

    private Type actionType;
    private String searchQuery;

    public enum Type {
        FETCHING, SEARCHING
    }

    public FetchCustomersAction(Type actionType,
                                String searchQuery) {
        this.actionType = actionType;
        this.searchQuery = searchQuery;
    }

    public Type getActionType() {
        return actionType;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

}
