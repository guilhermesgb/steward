package com.github.guilhermesgb.steward.mvi.table.schema;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

import io.reactivex.Single;

@Dao
public interface TableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Table> tables);

    @Query("DELETE FROM stand")
    void deleteAll();

    @Query("SELECT * FROM stand")
    Single<List<Table>> findAll();

    @Query("SELECT * FROM stand WHERE number = :number LIMIT 1")
    Single<Table> findByNumber(int number);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Table table);

}
