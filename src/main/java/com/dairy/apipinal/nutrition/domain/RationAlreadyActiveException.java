package com.dairy.apipinal.nutrition.domain;

public class RationAlreadyActiveException extends RuntimeException {

    public RationAlreadyActiveException() {
        super("Une autre ration active est déjà applicable à cet animal sur cette période.");
    }
}