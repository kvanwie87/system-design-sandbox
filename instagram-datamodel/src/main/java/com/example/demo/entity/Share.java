package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "shares")
public class Share {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	public Share() {}

	public Share(Post post, User user) {
		this.post = post;
		this.user = user;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Post getPost() { return post; }
	public void setPost(Post post) { this.post = post; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }

	@Override
	public String toString() {
		return "Share{id=" + id + ", postId=" + (post != null ? post.getId() : null) + ", userId=" + (user != null ? user.getId() : null) + "}";
	}
}
