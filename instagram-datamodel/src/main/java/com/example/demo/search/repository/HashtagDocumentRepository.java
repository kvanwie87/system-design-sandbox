package com.example.demo.search.repository;

import com.example.demo.search.document.HashtagDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.Optional;

public interface HashtagDocumentRepository extends ElasticsearchRepository<HashtagDocument, String> {
	Optional<HashtagDocument> findByHashtag(String hashtag);
}
