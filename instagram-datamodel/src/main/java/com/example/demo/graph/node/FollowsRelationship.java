package com.example.demo.graph.node;

import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
public class FollowsRelationship {

	@Id
	@GeneratedValue
	private Long id;

	private Double engagementScore;

	@TargetNode
	private UserNode targetUser;

	public FollowsRelationship() {}

	public FollowsRelationship(UserNode targetUser, Double engagementScore) {
		this.targetUser = targetUser;
		this.engagementScore = engagementScore;
	}

	public Long getId() { return id; }
	public void setId(Long id) { this.id = id; }
	public Double getEngagementScore() { return engagementScore; }
	public void setEngagementScore(Double engagementScore) { this.engagementScore = engagementScore; }
	public UserNode getTargetUser() { return targetUser; }
	public void setTargetUser(UserNode targetUser) { this.targetUser = targetUser; }

	@Override
	public String toString() {
		return "FOLLOWS{target=" + (targetUser != null ? targetUser.getUsername() : null) + ", score=" + engagementScore + "}";
	}
}
