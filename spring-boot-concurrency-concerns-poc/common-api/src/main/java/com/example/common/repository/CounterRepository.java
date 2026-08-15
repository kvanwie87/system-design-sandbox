package com.example.common.repository;

import com.example.common.entity.CounterEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CounterRepository extends JpaRepository<CounterEntity, Long> {

    Optional<CounterEntity> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM CounterEntity c WHERE c.name = :name")
    Optional<CounterEntity> findByNameForUpdate(@Param("name") String name);
}
