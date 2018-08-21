package com.github.guilhermesgb.steward.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.github.guilhermesgb.steward.R;
import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("WeakerAccess")
public class CustomerRenderer extends Renderer<CustomerItemView> {

    @BindView(R.id.customerNameText) TextView customerNameText;
    @BindView(R.id.touchSurface) View touchSurface;

    @Override
    protected void setUpView(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    @Override
    protected void hookListeners(View rootView) {}

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.renderer_customer, parent, false);
    }

    @Override
    public void render() {
        final Customer customer = getContent().getCustomer();
        customerNameText.setText(getContext().getString(R.string.format_customer_name,
            customer.getFirstName() + " " + customer.getLastName()));
        customerNameText.setSelected(true);
        touchSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                touchSurface.setEnabled(false);
                getContent().getCallback().onCustomerChosen(customer);
            }
        });
        touchSurface.setEnabled(true);
    }

}
