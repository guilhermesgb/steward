package com.github.guilhermesgb.steward.ui;

import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.guilhermesgb.steward.R;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.utils.FontAwesomeSolid;
import com.joanzapata.iconify.IconDrawable;
import com.pedrogomez.renderers.Renderer;

import butterknife.BindView;
import butterknife.ButterKnife;

@SuppressWarnings("WeakerAccess")
public class TableRenderer extends Renderer<TableItemView> {

    @BindView(R.id.tableImageView) View tableImageView;
    @BindView(R.id.tableAvailabilityIcon) ImageView tableAvailabilityIcon;
    @BindView(R.id.tableNameText) TextView tableNameText;
    @BindView(R.id.touchSurface) View touchSurface;

    @Override
    protected void setUpView(View rootView) {
        ButterKnife.bind(this, rootView);
    }

    @Override
    protected void hookListeners(View rootView) {}

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(R.layout.renderer_table, parent, false);
    }

    @Override
    public void render() {
        final Table table = getContent().getTable();
        if (table.isAvailable()) {
            tableImageView.setBackgroundResource(R.drawable.table_image_available);
            tableAvailabilityIcon.setImageDrawable
                (new IconDrawable(getContext(), FontAwesomeSolid.fa_s_lock_open)
                    .colorRes(R.color.colorTextPrimary).sizeDp(36));
            tableNameText.setTextColor(ContextCompat
                .getColor(getContext(), R.color.colorPrimary));
        } else {
            tableImageView.setBackgroundResource(R.drawable.table_image_unavailable);
            tableAvailabilityIcon.setImageDrawable
                (new IconDrawable(getContext(), FontAwesomeSolid.fa_s_lock)
                    .colorRes(R.color.colorAccent).sizeDp(36));
            tableNameText.setTextColor(ContextCompat
                .getColor(getContext(), R.color.colorAccent));
        }
        tableNameText.setText(getContext().getString
            (R.string.format_table_name, table.getNumber()));
        tableNameText.setSelected(true);
        touchSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getContent().getCallback().onTableChosen(table);
            }
        });
    }

}
