package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "comments")
public class Comment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "comment_text", nullable = false)
	private String commentText;

	public Comment() {}

	public Comment(Post post, User user, String commentText) {
		this.post = post;
		this.user = user;
		this.commentText = commentText;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Post getPost() { return post; }
	public void setPost(Post post) { this.post = post; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public String getCommentText() { return commentText; }
	public void setCommentText(String commentText) { this.commentText = commentText; }

	@Override
	public String toString() {
		return "Comment{id=" + id + ", text='" + commentText + "'}";
	}
}
