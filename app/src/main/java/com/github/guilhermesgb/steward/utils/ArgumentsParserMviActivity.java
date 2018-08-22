package com.github.guilhermesgb.steward.utils;

import android.content.Intent;
import android.os.Bundle;

import com.hannesdorfmann.mosby3.mvi.MviActivity;
import com.hannesdorfmann.mosby3.mvi.MviBasePresenter;
import com.hannesdorfmann.mosby3.mvp.MvpView;

import java.util.List;

/**
 * An Activity with built-in mechanisms for easily parsing arguments located within the starter
 * intent as well as the saved instance bundle which are passed down to this instance.
 * @param <V> the view which this activity implements
 * @param <P> the presenter which is bound to this activity and links it to the business logic
 */
public abstract class ArgumentsParserMviActivity
        <V extends MvpView, P extends MviBasePresenter<V, ?>>
            extends MviActivity<V, P> {

    private ArgumentsParser argumentsParser = new ArgumentsParser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intentWithExtras = getIntent();
        ArgumentsParsedCallback callback = defineResolvedArgumentValuesAssigner();
        if (callback != null) {
            callback.doUponArgumentParsingCompleted(argumentsParser.parseArguments
                (savedInstanceState, intentWithExtras, defineExpectedArguments()));
        }
    }

    @Override
    public void onNewIntent(Intent intentWithExtras) {
        super.onNewIntent(intentWithExtras);
        ArgumentsParsedCallback callback = defineResolvedArgumentValuesAssigner();
        if (callback != null) {
            callback.doUponArgumentParsingCompleted(argumentsParser.parseArguments
                (null, intentWithExtras, defineExpectedArguments()));
        }
    }

    protected abstract List<Argument> defineExpectedArguments();

    protected abstract ArgumentsParsedCallback defineResolvedArgumentValuesAssigner();

}
