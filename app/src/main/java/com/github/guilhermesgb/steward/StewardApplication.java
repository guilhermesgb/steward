package com.github.guilhermesgb.steward;

import android.app.Application;
import android.os.Looper;

import com.github.guilhermesgb.steward.utils.FontAwesomeBrands;
import com.github.guilhermesgb.steward.utils.FontAwesomeRegular;
import com.github.guilhermesgb.steward.utils.FontAwesomeSolid;
import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.concurrent.Callable;

import io.reactivex.Scheduler;
import io.reactivex.android.plugins.RxAndroidPlugins;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.plugins.RxJavaPlugins;
import retrofit2.HttpException;
import timber.log.Timber;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

public class StewardApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
        }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler(new Function<Callable<Scheduler>, Scheduler>() {
            @Override
            public Scheduler apply(Callable<Scheduler> schedulerCallable) {
                return AndroidSchedulers.from(Looper.getMainLooper(), true);
            }
        });
        RxJavaPlugins.setErrorHandler(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                if (!(throwable instanceof Exception && !(throwable instanceof RuntimeException))) {
                    if (throwable instanceof HttpException) {
                        return;
                    }
                    Thread.currentThread().getUncaughtExceptionHandler()
                        .uncaughtException(Thread.currentThread(), throwable);
                }
            }
        });
        JodaTimeAndroid.init(this);
        Iconify.with(new FontAwesomeModule() {
            @Override
            public String ttfFileName() {
                return "fonts/FontAwesome-Solid.ttf";
            }
            @Override
            public Icon[] characters() {
                return FontAwesomeSolid.values();
            }
        });
        Iconify.with(new FontAwesomeModule() {
            @Override
            public String ttfFileName() {
                return "fonts/FontAwesome-Regular.ttf";
            }
            @Override
            public Icon[] characters() {
                return FontAwesomeRegular.values();
            }
        });
        Iconify.with(new FontAwesomeModule() {
            @Override
            public String ttfFileName() {
                return "fonts/FontAwesome-Brands.ttf";
            }
            @Override
            public Icon[] characters() {
                return FontAwesomeBrands.values();
            }
        });
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
            .setDefaultFontPath("fonts/Arya-Regular.ttf")
            .build());
    }

}
