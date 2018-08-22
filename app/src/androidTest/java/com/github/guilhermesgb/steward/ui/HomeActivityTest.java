package com.github.guilhermesgb.steward.ui;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import com.github.guilhermesgb.steward.R;
import com.github.guilhermesgb.steward.utils.ItemViewAssertion;
import com.github.guilhermesgb.steward.utils.RecyclerItemViewAssertion;
import com.github.guilhermesgb.steward.utils.RecyclerViewItemCountAssertion;
import com.github.guilhermesgb.steward.utils.RendererItemView;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static com.github.guilhermesgb.steward.utils.TextColorMatcher.withTextColor;

@RunWith(AndroidJUnit4.class)
public class HomeActivityTest {

    @Rule
    public ActivityTestRule<HomeActivity> activityTestRule = new ActivityTestRule<>(HomeActivity.class);

    @Test
    public void testWholeReservationCreationProcess() {
        //Checks if all customers are being shown to the waiter.
        onView(withId(R.id.customersView))
            .check(new RecyclerViewItemCountAssertion(22)); //+1 since a space view is also an item among customer item views
        //Scrolls to some customer, and select it.
        onView(withId(R.id.customersView))
            .perform(actionOnItemAtPosition(15, click()));
        //Checks if selected customer is being highlighted.
        onView(withId(R.id.customersView))
            .check(new RecyclerViewItemCountAssertion(2)); //+1 since a space view is also an item among customer item views

        //Checks if all tables are being shown to the waiter.
        onView(withId(R.id.tablesView))
            .check(new RecyclerViewItemCountAssertion(71)); //+1 since a space view is also an item among table item views
        //Then selects some Table.
        onView(withId(R.id.tablesView))
            .perform(actionOnItemAtPosition(42, click()));
        //Checks if selected table is being highlighted.
        onView(withId(R.id.tablesView))
            .check(new RecyclerViewItemCountAssertion(2)); //+1 since a confirmation item is also displayed at this stage

        //Fires button to confirm reservation.
        onView(withId(R.id.tablesView))
            .perform(actionOnItemAtPosition(1, click()));

        //Checks if chosen table is now being shown as unavailable.
        onView(withId(R.id.tablesView))
            .perform(actionOnItemAtPosition(0, click()));

        onView(withId(R.id.tablesView))
            .perform(scrollToPosition(42))
            .check(new RecyclerItemViewAssertion<>(42, null,
                new ItemViewAssertion<RendererItemView>() {
                    @Override
                    public void check(RendererItemView item, View view, NoMatchingViewException exception) {
                        matches(hasDescendant(withTextColor(R.color.colorAccent,
                                activityTestRule.getActivity().getResources())))
                            .check(view, exception);
                    }
                }));

        //Proceed back to choosing next customer.
        onView(withId(R.id.customersView))
            .perform(actionOnItemAtPosition(1, click()));
    }

}
