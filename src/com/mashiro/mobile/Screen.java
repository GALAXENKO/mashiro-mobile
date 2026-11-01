package com.mashiro.mobile;

import android.view.View;

public abstract class Screen {
    protected final MainActivity a;
    private View root;

    protected Screen(MainActivity a) {
        this.a = a;
    }

    public final View view() {
        if (root == null) root = build();
        return root;
    }

    protected abstract View build();

    public void onShow() {}

    public void onHide() {}

    public void refresh() {}

    public boolean onBack() {
        return false;
    }
}
