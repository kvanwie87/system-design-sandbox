package com.example.demo.graph.repository;

import com.example.demo.graph.node.UserNode;
import org.springframework.data.neo4j.repository.Neo4jRepository;
import org.springframework.data.neo4j.repository.query.Query;

import java.util.List;
import java.util.Optional;

public interface UserNodeRepository extends Neo4jRepository<UserNode, Long> {
	Optional<UserNode> findByUserId(Long userId);
	Optional<UserNode> findByUsername(String username);

	@Query("MATCH (a:User)-[:FOLLOWS]->(b:User) WHERE a.userId = $userId RETURN b")
	List<UserNode> findFollowing(Long userId);

	@Query("MATCH (a:User)-[:FOLLOWS]->(b:User) WHERE b.userId = $userId RETURN a")
	List<UserNode> findFollowers(Long userId);
}
