package com.bank.model;

import java.util.Objects;

/**
 * Holder of an account. Demonstrates encapsulation:
 * all fields are private and exposed only through getters.
 */
public class Customer {

    private final String name;
    private final String phone;

    public Customer(String name, String phone) {
        this.name = name;
        this.phone = phone;
    }

    public String getName() {
        return name;
    }

    public String getPhone() {
        return phone;
    }

    @Override
    public String toString() {
        return name + " (" + phone + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Customer other)) return false;
        return name.equals(other.name) && phone.equals(other.phone);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, phone);
    }
}
