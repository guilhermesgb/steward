package com.github.guilhermesgb.steward.mvi.customer.schema;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import com.google.gson.JsonObject;

import java.io.Serializable;

import static com.github.guilhermesgb.steward.utils.JsonUtils.getOptionalString;

@Entity(tableName = "customer")
public class Customer implements Serializable, Parcelable {

    @PrimaryKey @NonNull private String id;
    private String firstName;
    private String lastName;

    private Customer(Parcel in) {
        id = in.readString();
        firstName = in.readString();
        lastName = in.readString();
    }

    public static final Creator<Customer> CREATOR = new Creator<Customer>() {
        @Override
        public Customer createFromParcel(Parcel in) {
            return new Customer(in);
        }

        @Override
        public Customer[] newArray(int size) {
            return new Customer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(firstName);
        dest.writeString(lastName);
    }

    Customer(@NonNull String id, String firstName, String lastName) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @NonNull
    public String getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public static Customer dejsonizeFrom(JsonObject json) {
        return json == null ? null : new Customer(
            getOptionalString(json, "id"),
            getOptionalString(json, "customerFirstName"),
            getOptionalString(json, "customerLastName")
        );
    }

    public static JsonObject jsonizeFrom(Customer customer) {
        if (customer == null) {
            return null;
        }
        JsonObject json = new JsonObject();
        json.addProperty("id", customer.id);
        json.addProperty("customerFirstName", customer.firstName);
        json.addProperty("customerLastName", customer.lastName);
        return json;
    }

    @Override
    public String toString() {
        return jsonizeFrom(this).toString();
    }

}
