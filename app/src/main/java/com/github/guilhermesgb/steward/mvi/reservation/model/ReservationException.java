package com.github.guilhermesgb.steward.mvi.reservation.model;

public class ReservationException extends Exception {

    private Code code;

    public enum Code {
        RESERVATION_IN_PLACE,
        CUSTOMER_BUSY,
        TABLE_UNAVAILABLE
    }

    public ReservationException(Code code) {
        this.code = code;
    }

    public Code getCode() {
        return code;
    }

}
