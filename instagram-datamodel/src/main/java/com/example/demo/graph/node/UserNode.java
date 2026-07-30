package com.example.demo.graph.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

import java.util.ArrayList;
import java.util.List;

@Node("User")
public class UserNode {

	@Id
	@GeneratedValue
	private Long id;

	private Long userId;
	private String username;

	@Relationship(type = "FOLLOWS", direction = Relationship.Direction.OUTGOING)
	private List<FollowsRelationship> following = new ArrayList<>();

	public UserNode() {}

	public UserNode(Long userId, String username) {
		this.userId = userId;
		this.username = username;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Long getUserId() { return userId; }
	public void setUserId(Long userId) { this.userId = userId; }
	public String getUsername() { return username; }
	public void setUsername(String username) { this.username = username; }
	public List<FollowsRelationship> getFollowing() { return following; }
	public void setFollowing(List<FollowsRelationship> following) { this.following = following; }

	@Override
	public String toString() {
		return "UserNode{id=" + id + ", userId=" + userId + ", username='" + username + "'}";
	}
}
