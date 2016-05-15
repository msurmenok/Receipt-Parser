package com.surmenok.maria.recepitparser;

/**
 * Created by Maria on 5/1/2016.
 */
public class Item {
    private String name;
    private double price;

    public Item(String name, double price) {
        this.name = name;
        this.price = price;
    }


    public String getName() {
        return name;
    }

    public double getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return String.format("%s = %.2f", name, price);
    }
}
