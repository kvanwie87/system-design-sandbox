package com.example.broken.repository;

import com.example.broken.entity.BrokenCounterEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BrokenCounterRepository extends JpaRepository<BrokenCounterEntity, Long> {

    Optional<BrokenCounterEntity> findByName(String name);
}
