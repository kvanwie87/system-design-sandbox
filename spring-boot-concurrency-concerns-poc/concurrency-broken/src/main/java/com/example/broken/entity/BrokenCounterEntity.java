package com.example.broken.entity;

import jakarta.persistence.*;

/**
 * Counter entity WITHOUT @Version — no optimistic locking protection.
 * This allows concurrent transactions to silently overwrite each other's changes.
 */
@Entity
@Table(name = "counters")
public class BrokenCounterEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "counter_value", nullable = false)
    private long value;

    public BrokenCounterEntity() {
    }

    public BrokenCounterEntity(String name, long value) {
        this.name = name;
        this.value = value;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }
}
