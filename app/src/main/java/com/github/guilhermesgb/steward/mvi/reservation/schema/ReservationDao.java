package com.github.guilhermesgb.steward.mvi.reservation.schema;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.RoomWarnings;

import com.github.guilhermesgb.steward.mvi.table.schema.Table;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface ReservationDao {

    @SuppressWarnings(RoomWarnings.CURSOR_MISMATCH)
    @Query("SELECT * FROM stand INNER JOIN reservation "
        + "ON stand.number=reservation.tableNumber "
        + "WHERE reservation.customerId=:customerId")
    Single<List<Table>> findTablesForGivenCustomer(String customerId);

    @Query("DELETE FROM reservation WHERE customerId LIKE :customerId")
    void deleteAllForCustomer(String customerId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Reservation reservation);

    @Query("DELETE FROM customer WHERE id NOT IN(SELECT reservation.customerId FROM reservation)")
    void deleteUnusedCustomers();

    @Query("DELETE FROM stand WHERE number NOT IN(SELECT reservation.tableNumber FROM reservation)")
    void deleteUnusedTables();

}
