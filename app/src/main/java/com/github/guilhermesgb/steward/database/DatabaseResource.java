package com.github.guilhermesgb.steward.database;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;

import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.customer.schema.CustomerDao;
import com.github.guilhermesgb.steward.mvi.reservation.schema.Reservation;
import com.github.guilhermesgb.steward.mvi.reservation.schema.ReservationDao;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;
import com.github.guilhermesgb.steward.mvi.table.schema.TableDao;

@Database(
    version = 1,
    exportSchema = false,
    entities = {
        Customer.class,
        Table.class,
        Reservation.class
    }
)
public abstract class DatabaseResource extends RoomDatabase {

    private static DatabaseResource instance;

    public static DatabaseResource getInstance(Context context) {
        if (instance == null && context != null) {
            instance = Room.databaseBuilder(context,
                DatabaseResource.class, "steward-db")
                    .build();
        }
        return instance;
    }

    public abstract CustomerDao customerDao();

    public abstract TableDao tableDao();

    public abstract ReservationDao reservationDao();

}
