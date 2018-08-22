package com.github.guilhermesgb.steward.ui;

import android.os.Parcel;
import android.os.Parcelable;

import com.github.guilhermesgb.steward.mvi.customer.schema.Customer;
import com.github.guilhermesgb.steward.mvi.reservation.model.ReservationException;
import com.github.guilhermesgb.steward.mvi.table.schema.Table;

import java.io.Serializable;
import java.util.List;

public class LastStateWithPrecedence implements Serializable, Parcelable {

    private int precedenceValue;
    private List<Customer> customers;
    private List<Table> tables;
    private Customer chosenCustomer;
    private Table chosenTable;
    private ReservationException exception;

    private LastStateWithPrecedence(Parcel in) {
        precedenceValue = in.readInt();
        customers = in.createTypedArrayList(Customer.CREATOR);
        tables = in.createTypedArrayList(Table.CREATOR);
        chosenCustomer = in.readParcelable(Customer.class.getClassLoader());
        chosenTable = in.readParcelable(Table.class.getClassLoader());
        exception = (ReservationException) in.readSerializable();
    }

    public static final Creator<LastStateWithPrecedence> CREATOR = new Creator<LastStateWithPrecedence>() {
        @Override
        public LastStateWithPrecedence createFromParcel(Parcel in) {
            return new LastStateWithPrecedence(in);
        }

        @Override
        public LastStateWithPrecedence[] newArray(int size) {
            return new LastStateWithPrecedence[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(precedenceValue);
        parcel.writeTypedList(customers);
        parcel.writeTypedList(tables);
        parcel.writeParcelable(chosenCustomer, i);
        parcel.writeParcelable(chosenTable, i);
        parcel.writeSerializable(exception);
    }

    public LastStateWithPrecedence(int precedenceValue, List<Customer> customers,
                                   List<Table> tables, Customer chosenCustomer,
                                   Table chosenTable, ReservationException exception) {
        this.precedenceValue = precedenceValue;
        this.customers = customers;
        this.tables = tables;
        this.chosenCustomer = chosenCustomer;
        this.chosenTable = chosenTable;
        this.exception = exception;
    }

    public int getPrecedenceValue() {
        return precedenceValue;
    }

    public List<Customer> getCustomers() {
        return customers;
    }

    public List<Table> getTables() {
        return tables;
    }

    public Customer getChosenCustomer() {
        return chosenCustomer;
    }

    public Table getChosenTable() {
        return chosenTable;
    }

    public ReservationException getException() {
        return exception;
    }

}
