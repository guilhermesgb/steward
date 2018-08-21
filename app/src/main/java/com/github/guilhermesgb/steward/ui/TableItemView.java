package com.github.guilhermesgb.steward.ui;

import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.utils.RendererItemView;

public abstract class TableItemView implements RendererItemView {

    private final Table table;
    private TableChosenCallback callback;

    public interface TableChosenCallback {

        void onTableChosen(Table table);

    }

    TableItemView(Table table, TableChosenCallback callback) {
        this.table = table;
        this.callback = callback;
    }

    public Table getTable() {
        return table;
    }

    public TableChosenCallback getCallback() {
        return callback;
    }

}
