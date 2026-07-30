package com.example.demo.repository;

import com.example.demo.entity.Follower;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowerRepository extends JpaRepository<Follower, Long> {
	List<Follower> findByFollowee(User followee);
}
