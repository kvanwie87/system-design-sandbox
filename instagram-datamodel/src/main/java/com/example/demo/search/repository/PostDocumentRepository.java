package com.example.demo.search.repository;

import com.example.demo.search.document.PostDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface PostDocumentRepository extends ElasticsearchRepository<PostDocument, String> {
	List<PostDocument> findByUsername(String username);
	List<PostDocument> findByCaptionContaining(String keyword);
}
