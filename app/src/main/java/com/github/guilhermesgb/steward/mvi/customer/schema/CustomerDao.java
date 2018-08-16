package com.github.guilhermesgb.steward.mvi.customer.schema;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Customer> customers);

    @Query("DELETE FROM customer")
    void deleteAll();

    @Query("SELECT * FROM customer")
    Single<List<Customer>> findAll();

}
