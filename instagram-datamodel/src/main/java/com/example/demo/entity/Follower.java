package com.example.demo.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "followers")
public class Follower {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "followee_id", nullable = false)
	private User followee;

	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;

	public Follower() {}

	public Follower(User followee, LocalDateTime createdAt) {
		this.followee = followee;
		this.createdAt = createdAt;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public User getFollowee() { return followee; }
	public void setFollowee(User followee) { this.followee = followee; }
	public LocalDateTime getCreatedAt() { return createdAt; }
	public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

	@Override
	public String toString() {
		return "Follower{id=" + id + ", followeeId=" + (followee != null ? followee.getId() : null) + ", createdAt=" + createdAt + "}";
	}
}
