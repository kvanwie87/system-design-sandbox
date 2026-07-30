package com.example.demo.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.util.List;

@Document(indexName = "hashtags")
public class HashtagDocument {

	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private String hashtag;

	@Field(type = FieldType.Long)
	private List<Long> postIds;

	@Field(type = FieldType.Integer)
	private int postCount;

	public HashtagDocument() {}

	public HashtagDocument(String hashtag, List<Long> postIds) {
		this.id = "tag-" + hashtag;
		this.hashtag = hashtag;
		this.postIds = postIds;
		this.postCount = postIds.size();
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public String getHashtag() { return hashtag; }
	public void setHashtag(String hashtag) { this.hashtag = hashtag; }
	public List<Long> getPostIds() { return postIds; }
	public void setPostIds(List<Long> postIds) { this.postIds = postIds; }
	public int getPostCount() { return postCount; }
	public void setPostCount(int postCount) { this.postCount = postCount; }

	@Override
	public String toString() {
		return "HashtagDocument{hashtag='" + hashtag + "', postCount=" + postCount + "}";
	}
}
