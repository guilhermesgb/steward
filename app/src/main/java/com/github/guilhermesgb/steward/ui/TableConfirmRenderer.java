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
public class TableConfirmRenderer extends Renderer<TableItemView> {

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
        tableImageView.setBackgroundResource(R.drawable.table_image_confirm);
        tableAvailabilityIcon.setImageDrawable
            (new IconDrawable(getContext(), FontAwesomeSolid.fa_s_check)
                .colorRes(R.color.colorAccentSecondary).sizeDp(36));
        tableNameText.setTextColor(ContextCompat
            .getColor(getContext(), R.color.colorAccentSecondary));
        tableNameText.setText(R.string.label_confirm_reservation);
        tableNameText.setSelected(true);
        touchSurface.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                touchSurface.setEnabled(false);
                getContent().getCallback().onTableChosen(table);
            }
        });
        touchSurface.setEnabled(true);
    }

}
