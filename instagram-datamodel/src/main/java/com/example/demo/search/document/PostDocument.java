package com.example.demo.search.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Document(indexName = "posts")
public class PostDocument {

	@Id
	private String id;

	@Field(type = FieldType.Long)
	private Long postId;

	@Field(type = FieldType.Text, analyzer = "standard")
	private String username;

	@Field(type = FieldType.Text, analyzer = "standard")
	private String caption;

	@Field(type = FieldType.Integer)
	private int likesCount;

	public PostDocument() {}

	public PostDocument(Long postId, String username, String caption, int likesCount) {
		this.id = "post-" + postId;
		this.postId = postId;
		this.username = username;
		this.caption = caption;
		this.likesCount = likesCount;
	}

	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	public Long getPostId() { return postId; }
	public void setPostId(Long postId) { this.postId = postId; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public String getCaption() { return caption; }
	public void setCaption(String caption) { this.caption = caption; }
	public int getLikesCount() { return likesCount; }
	public void setLikesCount(int likesCount) { this.likesCount = likesCount; }

	@Override
	public String toString() {
		return "PostDocument{postId=" + postId + ", username='" + username + "', caption='" + caption + "'}";
	}
}
