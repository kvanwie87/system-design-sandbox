package com.example.telemetry.processor.repository;

import com.example.telemetry.processor.entity.SensorReadingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SensorReadingRepository extends JpaRepository<SensorReadingEntity, Long> {

    List<SensorReadingEntity> findBySensorId(String sensorId);

    List<SensorReadingEntity> findBySensorIdOrderByTimestampDesc(String sensorId);

    long countBySensorId(String sensorId);
}
