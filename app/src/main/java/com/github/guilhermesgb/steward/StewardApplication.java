package com.github.guilhermesgb.steward;

import android.app.Application;
import android.os.Looper;
import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.evernote.android.job.JobCreator;
import com.evernote.android.job.JobManager;
import com.evernote.android.job.JobRequest;
import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.utils.FontAwesomeBrands;
import com.github.guilhermesgb.steward.utils.FontAwesomeRegular;
import com.github.guilhermesgb.steward.utils.FontAwesomeSolid;
import com.github.guilhermesgb.steward.worker.ReservationsCleanupWorker;
import com.joanzapata.iconify.Icon;
import com.joanzapata.iconify.Iconify;
import com.joanzapata.iconify.fonts.FontAwesomeModule;

import net.danlew.android.joda.JodaTimeAndroid;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

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
        JobManager.create(this).addJobCreator(new JobCreator() {
            @Override
            public Job create(@NonNull String tag) {
                switch (tag) {
                    case ReservationsCleanupWorker.TAG:
                        return new ReservationsCleanupWorker
                            (DatabaseResource.getInstance
                                (getApplicationContext()));
                    default:
                        return null;
                }
            }
        });
        new JobRequest.Builder(ReservationsCleanupWorker.TAG)
            .setPeriodic(TimeUnit.MINUTES.toMillis(15), //Minimum is 15 minutes
                TimeUnit.MINUTES.toMillis(10))         //but we set the flexMs to 10 min
            .setUpdateCurrent(true)
            .build()                                           //so that it may try running the required
            .schedule();                                      //reservations cleanup work around this time.
    }

}
