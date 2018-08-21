package com.github.guilhermesgb.steward.ui;

import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.utils.RendererItemView;

public abstract class CustomerItemView implements RendererItemView {

    private final Customer customer;
    private final CustomerChosenCallback callback;

    public interface CustomerChosenCallback {

        void onCustomerChosen(Customer customer);

    }

    CustomerItemView(Customer customer, CustomerChosenCallback callback) {
        this.customer = customer;
        this.callback = callback;
    }

    public Customer getCustomer() {
        return customer;
    }

    public CustomerChosenCallback getCallback() {
        return callback;
    }

}
