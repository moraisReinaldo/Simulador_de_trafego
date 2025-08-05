package com.simuladortrafego;

public class Street {
    private String id;
    private double length; // in meters or an abstract unit
    private int capacity; // max number of cars
    private boolean isTwoWay;
    private Direction direction; // For one-way streets

    public Street(String id, double length, int capacity, boolean isTwoWay, Direction direction) {
        this.id = id;
        this.length = length;
        this.capacity = capacity;
        this.isTwoWay = isTwoWay;
        if (!isTwoWay && direction == null) {
            throw new IllegalArgumentException("Direction must be specified for one-way streets.");
        }
        this.direction = isTwoWay ? null : direction;
    }

    public String getId() {
        return id;
    }

    public double getLength() {
        return length;
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isTwoWay() {
        return isTwoWay;
    }

    public Direction getDirection() {
        if (isTwoWay) {
            System.out.println("Warning: Accessing direction for a two-way street. It will return null.");
        }
        return direction;
    }

    // Basic methods for cars entering/leaving the street might be added later
    // or managed by the Grid/Intersection classes.

    @Override
    public String toString() {
        return "Street{" +
                "id='" + id + '\'' +
                ", length=" + length +
                ", capacity=" + capacity +
                ", isTwoWay=" + isTwoWay +
                (isTwoWay ? "" : ", direction=" + direction) +
                '}';
    }
}

