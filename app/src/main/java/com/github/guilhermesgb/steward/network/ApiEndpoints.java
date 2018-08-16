package com.github.guilhermesgb.steward.network;

import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;

import java.util.List;

import retrofit2.http.GET;

public interface ApiEndpoints {

    @GET("quandoo-assessment/customer-list.json")
    List<Customer> fetchCustomers();

    @GET("quandoo-assessment/table-map.json")
    List<Table> fetchTables();

}
