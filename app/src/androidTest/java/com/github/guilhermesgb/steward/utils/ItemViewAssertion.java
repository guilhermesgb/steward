package com.github.guilhermesgb.steward.utils;

import android.support.test.espresso.NoMatchingViewException;
import android.view.View;

public interface ItemViewAssertion<A> {

    void check(A item, View view, NoMatchingViewException exception);

}
