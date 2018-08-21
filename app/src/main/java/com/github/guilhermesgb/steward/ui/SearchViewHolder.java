package com.github.guilhermesgb.steward.ui;

import android.support.v7.widget.SearchView;
import android.widget.ImageView;

import butterknife.BindView;

public class SearchViewHolder {

    @BindView(android.support.v7.appcompat.R.id.search_button) public ImageView searchIcon;
    @BindView(android.support.v7.appcompat.R.id.search_close_btn) public ImageView closeIcon;
    @BindView(android.support.v7.appcompat.R.id.search_src_text) public SearchView.SearchAutoComplete searchAutoComplete;

}
