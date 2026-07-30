package com.example.demo.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "media")
public class Media {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "post_id", nullable = false)
	private Post post;

	@Enumerated(EnumType.STRING)
	@Column(name = "media_type", nullable = false)
	private MediaType mediaType;

	@Column(name = "media_url", nullable = false)
	private String mediaUrl;

	public Media() {}

	public Media(Post post, MediaType mediaType, String mediaUrl) {
		this.post = post;
		this.mediaType = mediaType;
		this.mediaUrl = mediaUrl;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Post getPost() { return post; }
	public void setPost(Post post) { this.post = post; }
	public MediaType getMediaType() { return mediaType; }
	public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
	public String getMediaUrl() { return mediaUrl; }
	public void setMediaUrl(String mediaUrl) { this.mediaUrl = mediaUrl; }

	@Override
	public String toString() {
		return "Media{id=" + id + ", type=" + mediaType + ", url='" + mediaUrl + "'}";
	}
}
