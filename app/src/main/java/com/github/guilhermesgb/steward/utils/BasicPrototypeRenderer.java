package com.github.guilhermesgb.steward.utils;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.pedrogomez.renderers.Renderer;

public abstract class BasicPrototypeRenderer extends Renderer<RendererItemView> {

    @Override
    protected void setUpView(View rootView) {}

    @Override
    protected void hookListeners(View rootView) {}

    @Override
    protected View inflate(LayoutInflater inflater, ViewGroup parent) {
        return inflater.inflate(getPrototypeResourceId(), parent, false);
    }

    @Override
    public void render() {}

    public abstract int getPrototypeResourceId();

}
