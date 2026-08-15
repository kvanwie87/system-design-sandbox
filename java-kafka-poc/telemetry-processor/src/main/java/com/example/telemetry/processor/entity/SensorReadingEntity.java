package com.example.telemetry.processor.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "sensor_readings")
public class SensorReadingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sensorId;

    @Column(nullable = false)
    private double temperature;

    @Column(nullable = false)
    private double humidity;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String location;

    @Column(nullable = false)
    private int kafkaPartition;

    @Column(nullable = false)
    private long kafkaOffset;

    public SensorReadingEntity() {}

    public SensorReadingEntity(String sensorId, double temperature, double humidity,
                               Instant timestamp, String location,
                               int kafkaPartition, long kafkaOffset) {
        this.sensorId = sensorId;
        this.temperature = temperature;
        this.humidity = humidity;
        this.timestamp = timestamp;
        this.location = location;
        this.kafkaPartition = kafkaPartition;
        this.kafkaOffset = kafkaOffset;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSensorId() { return sensorId; }
    public void setSensorId(String sensorId) { this.sensorId = sensorId; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public int getKafkaPartition() { return kafkaPartition; }
    public void setKafkaPartition(int kafkaPartition) { this.kafkaPartition = kafkaPartition; }

    public long getKafkaOffset() { return kafkaOffset; }
    public void setKafkaOffset(long kafkaOffset) { this.kafkaOffset = kafkaOffset; }

    @Override
    public String toString() {
        return "SensorReadingEntity{" +
                "id=" + id +
                ", sensorId='" + sensorId + '\'' +
                ", temperature=" + temperature +
                ", humidity=" + humidity +
                ", timestamp=" + timestamp +
                ", location='" + location + '\'' +
                ", partition=" + kafkaPartition +
                ", offset=" + kafkaOffset +
                '}';
    }
}
