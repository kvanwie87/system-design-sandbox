package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "posts")
public class Post {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(name = "likes_count")
	private int likesCount;

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<Media> mediaList = new ArrayList<>();

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<Comment> comments = new ArrayList<>();

	@OneToMany(mappedBy = "post", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	private List<Share> shares = new ArrayList<>();

	public Post() {}

	public Post(User user, int likesCount) {
		this.user = user;
		this.likesCount = likesCount;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public User getUser() { return user; }
	public void setUser(User user) { this.user = user; }
	public int getLikesCount() { return likesCount; }
	public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
	public List<Media> getMediaList() { return mediaList; }
	public void setMediaList(List<Media> mediaList) { this.mediaList = mediaList; }
	public List<Comment> getComments() { return comments; }
	public void setComments(List<Comment> comments) { this.comments = comments; }
	public List<Share> getShares() { return shares; }
	public void setShares(List<Share> shares) { this.shares = shares; }

	@Override
	public String toString() {
		return "Post{id=" + id + ", userId=" + (user != null ? user.getId() : null) + ", likesCount=" + likesCount + "}";
	}
}
