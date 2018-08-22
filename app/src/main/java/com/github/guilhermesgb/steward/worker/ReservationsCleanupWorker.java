package com.github.guilhermesgb.steward.worker;

import android.support.annotation.NonNull;

import com.evernote.android.job.Job;
import com.github.guilhermesgb.steward.database.DatabaseResource;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;

import java.util.List;

import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class ReservationsCleanupWorker extends Job {

    public static final String TAG = "reservations_cleanup_worker";

    private final DatabaseResource database;

    public ReservationsCleanupWorker(DatabaseResource database) {
        this.database = database;
    }

    @NonNull
    @Override
    protected Result onRunJob(@NonNull Params params) {
        Timber.wtf("WTF? reservations cleanup work fired!");
        //Removes all reservations and makes all tables available all in one sweep.
        try {
            database.beginTransaction();
            database.reservationDao().deleteAll();
            database.tableDao().findAll().toObservable()
                .doOnNext(new Consumer<List<Table>>() {
                    @Override
                    public void accept(List<Table> tables) {
                        for (Table table : tables) {
                            table.setAvailable(true);
                        }
                        database.tableDao().insertAll(tables);
                    }
                })
                .subscribeOn(Schedulers.io())
                .subscribe();
            database.setTransactionSuccessful();
            Timber.wtf("WTF? reservations cleanup work was marked successful.");
            return Result.SUCCESS;
        } catch (Throwable throwable) {
            Timber.wtf("WTF? reservations cleanup work was marked a failure.");
            return Result.FAILURE;
        } finally {
            database.endTransaction();
            Timber.wtf("WTF? reservations cleanup work finished!");
        }
    }

}
