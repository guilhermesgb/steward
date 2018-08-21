package com.github.guilhermesgb.steward.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.guilhermesgb.steward.R;

public class CustomerDividerRenderer extends CustomerRenderer {

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.renderer_customer_with_divider, parent, false);
    }

}
