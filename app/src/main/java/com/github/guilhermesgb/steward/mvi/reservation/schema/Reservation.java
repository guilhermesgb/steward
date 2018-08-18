package com.github.guilhermesgb.steward.mvi.reservation.schema;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.ForeignKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;

import java.io.Serializable;

@Entity(
    tableName = "reservation",
    primaryKeys = {"customerId", "tableNumber"},
    foreignKeys = {
        @ForeignKey(entity = Customer.class, parentColumns = "id", childColumns = "customerId"),
        @ForeignKey(entity = Table.class, parentColumns = "number", childColumns = "tableNumber")
    }
)
public class Reservation implements Serializable, Parcelable {

    @NonNull private final String customerId;
    private final int tableNumber;
    private String expirationDate;

    private Reservation(Parcel in) {
        customerId = in.readString();
        tableNumber = in.readInt();
        expirationDate = in.readString();
    }

    public static final Creator<Reservation> CREATOR = new Creator<Reservation>() {
        @Override
        public Reservation createFromParcel(Parcel in) {
            return new Reservation(in);
        }

        @Override
        public Reservation[] newArray(int size) {
            return new Reservation[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(customerId);
        dest.writeInt(tableNumber);
        dest.writeString(expirationDate);
    }

    public Reservation(@NonNull String customerId, int tableNumber, String expirationDate) {
        this.customerId = customerId;
        this.tableNumber = tableNumber;
        this.expirationDate = expirationDate;
    }

    @NonNull
    public String getCustomerId() {
        return customerId;
    }

    public int getTableNumber() {
        return tableNumber;
    }

    public String getExpirationDate() {
        return expirationDate;
    }

}
