package com.example.demo;

import com.example.demo.cache.FeedCacheService;
import com.example.demo.entity.*;
import com.example.demo.graph.node.FollowsRelationship;
import com.example.demo.graph.node.UserNode;
import com.example.demo.graph.repository.UserNodeRepository;
import com.example.demo.repository.*;
import com.example.demo.search.document.HashtagDocument;
import com.example.demo.search.document.PostDocument;
import com.example.demo.search.repository.HashtagDocumentRepository;
import com.example.demo.search.repository.PostDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DataLoader implements ApplicationRunner {

	private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

	private final UserRepository userRepository;
	private final PostRepository postRepository;
	private final MediaRepository mediaRepository;
	private final CommentRepository commentRepository;
	private final ShareRepository shareRepository;
	private final FollowerRepository followerRepository;
	private final UserNodeRepository userNodeRepository;
	private final PostDocumentRepository postDocumentRepository;
	private final HashtagDocumentRepository hashtagDocumentRepository;
	private final FeedCacheService feedCacheService;

	public DataLoader(UserRepository userRepository,
					  PostRepository postRepository,
					  MediaRepository mediaRepository,
					  CommentRepository commentRepository,
					  ShareRepository shareRepository,
					  FollowerRepository followerRepository,
					  UserNodeRepository userNodeRepository,
					  PostDocumentRepository postDocumentRepository,
					  HashtagDocumentRepository hashtagDocumentRepository,
					  FeedCacheService feedCacheService) {
		this.userRepository = userRepository;
		this.postRepository = postRepository;
		this.mediaRepository = mediaRepository;
		this.commentRepository = commentRepository;
		this.shareRepository = shareRepository;
		this.followerRepository = followerRepository;
		this.userNodeRepository = userNodeRepository;
		this.postDocumentRepository = postDocumentRepository;
		this.hashtagDocumentRepository = hashtagDocumentRepository;
		this.feedCacheService = feedCacheService;
	}

	@Override
	public void run(ApplicationArguments args) {
		log.info("========================================");
		log.info("=== Starting Data Seeding ===");
		log.info("========================================");

		seedJpaData();
		seedNeo4jData();
		seedElasticsearchData();
		seedRedisCache();

		log.info("========================================");
		log.info("=== Running Sample Queries ===");
		log.info("========================================");

		runJpaQueries();
		runNeo4jQueries();
		runElasticsearchQueries();
		runRedisCacheQueries();

		log.info("========================================");
		log.info("=== Data Seeding & Queries Complete ===");
		log.info("========================================");
	}

	private void seedJpaData() {
		log.info("--- Seeding JPA (H2) Data ---");

		// Users
		User alice = userRepository.save(new User("alice", "alice@example.com", "hash_alice"));
		User bob = userRepository.save(new User("bob", "bob@example.com", "hash_bob"));
		User charlie = userRepository.save(new User("charlie", "charlie@example.com", "hash_charlie"));
		log.info("Seeded {} users: alice, bob, charlie", userRepository.count());

		// Posts
		Post post1 = postRepository.save(new Post(alice, 42));
		Post post2 = postRepository.save(new Post(alice, 15));
		Post post3 = postRepository.save(new Post(bob, 88));
		log.info("Seeded {} posts", postRepository.count());

		// Media
		mediaRepository.save(new Media(post1, MediaType.PHOTO, "https://cdn.example.com/photos/sunset.jpg"));
		mediaRepository.save(new Media(post1, MediaType.VIDEO, "https://cdn.example.com/videos/timelapse.mp4"));
		mediaRepository.save(new Media(post3, MediaType.PHOTO, "https://cdn.example.com/photos/coding.jpg"));
		log.info("Seeded {} media items", mediaRepository.count());

		// Comments
		commentRepository.save(new Comment(post1, bob, "Beautiful sunset!"));
		commentRepository.save(new Comment(post1, charlie, "Amazing shot alice!"));
		commentRepository.save(new Comment(post3, alice, "Nice coding setup bob!"));
		log.info("Seeded {} comments", commentRepository.count());

		// Shares
		shareRepository.save(new Share(post1, charlie));
		shareRepository.save(new Share(post3, alice));
		log.info("Seeded {} shares", shareRepository.count());

		// Followers (bob and charlie follow alice)
		followerRepository.save(new Follower(alice, LocalDateTime.now().minusDays(30)));
		followerRepository.save(new Follower(alice, LocalDateTime.now().minusDays(10)));
		log.info("Seeded {} follower records", followerRepository.count());
	}

	private void seedNeo4jData() {
		log.info("--- Seeding Neo4j Data ---");

		UserNode aliceNode = new UserNode(1L, "alice");
		UserNode bobNode = new UserNode(2L, "bob");
		UserNode charlieNode = new UserNode(3L, "charlie");

		// bob follows alice (engagement score 0.85)
		bobNode.getFollowing().add(new FollowsRelationship(aliceNode, 0.85));
		// charlie follows alice (engagement score 0.72)
		charlieNode.getFollowing().add(new FollowsRelationship(aliceNode, 0.72));
		// alice follows bob (engagement score 0.60)
		aliceNode.getFollowing().add(new FollowsRelationship(bobNode, 0.60));

		userNodeRepository.save(aliceNode);
		userNodeRepository.save(bobNode);
		userNodeRepository.save(charlieNode);
		log.info("Seeded {} user nodes with FOLLOWS relationships", userNodeRepository.count());
	}

	private void seedElasticsearchData() {
		log.info("--- Seeding Elasticsearch Data ---");

		// Post documents
		postDocumentRepository.save(new PostDocument(1L, "alice", "Beautiful sunset at the beach #sunset #nature", 42));
		postDocumentRepository.save(new PostDocument(2L, "alice", "Morning coffee vibes #morning", 15));
		postDocumentRepository.save(new PostDocument(3L, "bob", "Late night coding session #coding #developer", 88));
		log.info("Seeded {} post documents", postDocumentRepository.count());

		// Hashtag documents
		hashtagDocumentRepository.save(new HashtagDocument("sunset", List.of(1L)));
		hashtagDocumentRepository.save(new HashtagDocument("nature", List.of(1L)));
		hashtagDocumentRepository.save(new HashtagDocument("morning", List.of(2L)));
		hashtagDocumentRepository.save(new HashtagDocument("coding", List.of(3L)));
		hashtagDocumentRepository.save(new HashtagDocument("developer", List.of(3L)));
		log.info("Seeded {} hashtag documents", hashtagDocumentRepository.count());
	}

	private void seedRedisCache() {
		log.info("--- Seeding Redis Cache (in-memory stub) ---");

		// Bob's feed: alice's posts (most recent first)
		feedCacheService.addToFeed(2L, 1L);
		feedCacheService.addToFeed(2L, 2L);

		// Charlie's feed: alice's posts
		feedCacheService.addToFeed(3L, 1L);
		feedCacheService.addToFeed(3L, 2L);

		log.info("Seeded feed cache for bob ({} posts) and charlie ({} posts)",
				feedCacheService.getFeedSize(2L), feedCacheService.getFeedSize(3L));
	}

	private void runJpaQueries() {
		log.info("--- JPA Queries ---");

		// Find user by username
		userRepository.findByUsername("alice").ifPresent(user ->
				log.info("findByUsername('alice'): {}", user));

		// Find posts by user
		User alice = userRepository.findByUsername("alice").orElseThrow();
		List<Post> alicePosts = postRepository.findByUser(alice);
		log.info("findByUser(alice): {} posts -> {}", alicePosts.size(), alicePosts);

		// Find popular posts (likes > 20)
		List<Post> popularPosts = postRepository.findByLikesCountGreaterThan(20);
		log.info("findByLikesCountGreaterThan(20): {} posts -> {}", popularPosts.size(), popularPosts);

		// Find photos
		List<Media> photos = mediaRepository.findByMediaType(MediaType.PHOTO);
		log.info("findByMediaType(PHOTO): {} items -> {}", photos.size(), photos);

		// Find comments on post1
		Post post1 = postRepository.findById(1L).orElseThrow();
		List<Comment> post1Comments = commentRepository.findByPost(post1);
		log.info("findByPost(post1): {} comments -> {}", post1Comments.size(), post1Comments);

		// Find shares by charlie
		User charlie = userRepository.findByUsername("charlie").orElseThrow();
		List<Share> charlieShares = shareRepository.findByUser(charlie);
		log.info("findByUser(charlie) shares: {} -> {}", charlieShares.size(), charlieShares);

		// Find followers of alice
		List<Follower> aliceFollowers = followerRepository.findByFollowee(alice);
		log.info("findByFollowee(alice): {} followers -> {}", aliceFollowers.size(), aliceFollowers);
	}

	private void runNeo4jQueries() {
		log.info("--- Neo4j Queries ---");

		// Who does bob follow?
		List<UserNode> bobFollowing = userNodeRepository.findFollowing(2L);
		log.info("findFollowing(bob): {} -> {}", bobFollowing.size(), bobFollowing);

		// Who follows alice?
		List<UserNode> aliceFollowers = userNodeRepository.findFollowers(1L);
		log.info("findFollowers(alice): {} -> {}", aliceFollowers.size(), aliceFollowers);

		// Find user node by username
		userNodeRepository.findByUsername("charlie").ifPresent(node ->
				log.info("findByUsername('charlie'): {}", node));
	}

	private void runElasticsearchQueries() {
		log.info("--- Elasticsearch Queries ---");

		// Search posts containing "sunset"
		List<PostDocument> sunsetPosts = postDocumentRepository.findByCaptionContaining("sunset");
		log.info("findByCaptionContaining('sunset'): {} -> {}", sunsetPosts.size(), sunsetPosts);

		// Search posts by username
		List<PostDocument> aliceDocs = postDocumentRepository.findByUsername("alice");
		log.info("findByUsername('alice'): {} -> {}", aliceDocs.size(), aliceDocs);

		// Find hashtag
		hashtagDocumentRepository.findByHashtag("coding").ifPresent(doc ->
				log.info("findByHashtag('coding'): {}", doc));
	}

	private void runRedisCacheQueries() {
		log.info("--- Redis Cache Queries ---");

		// Get bob's feed
		List<Long> bobFeed = feedCacheService.getFeed(2L);
		log.info("getFeed(bob): {} posts -> {}", bobFeed.size(), bobFeed);

		// Get charlie's feed
		List<Long> charlieFeed = feedCacheService.getFeed(3L);
		log.info("getFeed(charlie): {} posts -> {}", charlieFeed.size(), charlieFeed);

		// Get feed size
		log.info("getFeedSize(bob): {}", feedCacheService.getFeedSize(2L));
	}
}
