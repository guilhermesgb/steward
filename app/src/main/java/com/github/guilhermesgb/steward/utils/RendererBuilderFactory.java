package com.github.guilhermesgb.steward.utils;

import android.util.SparseArray;

import com.pedrogomez.renderers.Renderer;
import com.pedrogomez.renderers.RendererBuilder;

import java.util.LinkedList;
import java.util.List;

public class RendererBuilderFactory<T extends RendererItemView> {

    private List<Renderer<? extends T>> basicPrototypes = new LinkedList<>();
    private SparseArray<Class> bindings = new SparseArray<>();

    public RendererBuilderFactory<T> bind(int viewCode, Renderer<? extends T> renderer) {
        basicPrototypes.add(renderer);
        bindings.put(viewCode, renderer.getClass());
        return this;
    }

    public RendererBuilder<T> build() {
        RendererBuilder<T> rendererBuilder = new RendererBuilder<T>() {
            @Override
            protected Class getPrototypeClass(T itemView) {
                Class prototypeClass = bindings.get(itemView.getItemViewCode());
                if (prototypeClass == null) {
                    prototypeClass = super.getPrototypeClass(itemView);
                }
                return prototypeClass;
            }
        };
        rendererBuilder.setPrototypes(basicPrototypes);
        return rendererBuilder;
    }

}