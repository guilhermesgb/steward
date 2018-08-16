package com.github.guilhermesgb.steward.network;

import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.table.schema.Tables;

import java.util.List;

import io.reactivex.Single;
import retrofit2.http.GET;

public interface ApiEndpoints {

    @GET("quandoo-assessment/customer-list.json")
    Single<List<Customer>> fetchCustomers();

    @GET("quandoo-assessment/table-map.json")
    Single<Tables> fetchTables();

}
