package com.example.demo.repository;

import com.example.demo.entity.Post;
import com.example.demo.entity.Share;
import com.example.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShareRepository extends JpaRepository<Share, Long> {
	List<Share> findByPost(Post post);
	List<Share> findByUser(User user);
}
