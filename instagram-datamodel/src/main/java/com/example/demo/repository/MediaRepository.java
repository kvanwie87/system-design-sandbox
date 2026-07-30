package com.example.demo.repository;

import com.example.demo.entity.Media;
import com.example.demo.entity.MediaType;
import com.example.demo.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MediaRepository extends JpaRepository<Media, Long> {
	List<Media> findByPost(Post post);
	List<Media> findByMediaType(MediaType mediaType);
}
