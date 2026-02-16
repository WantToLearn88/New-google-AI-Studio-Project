package com.example.jamiya;

public class Member {
    private int id;
    private String name;
    private int order;
    private boolean isPaidForCurrentMonth; // Helper for UI binding

    public Member() {}

    public Member(int id, String name, int order) {
        this.id = id;
        this.name = name;
        this.order = order;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getOrder() { return order; }
    public void setOrder(int order) { this.order = order; }

    public boolean isPaidForCurrentMonth() { return isPaidForCurrentMonth; }
    public void setPaidForCurrentMonth(boolean paidForCurrentMonth) { isPaidForCurrentMonth = paidForCurrentMonth; }
}
