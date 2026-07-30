package com.example.demo.cache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * User feed cache backed by Spring Data Redis.
 * Key pattern: feed:{userId} -> list of post IDs (most recent first).
 */
@Service
public class FeedCacheService {

	private static final Logger log = LoggerFactory.getLogger(FeedCacheService.class);

	private final RedisTemplate<String, Long> redisTemplate;
	private final ListOperations<String, Long> listOps;

	public FeedCacheService(RedisTemplate<String, Long> redisTemplate) {
		this.redisTemplate = redisTemplate;
		this.listOps = redisTemplate.opsForList();
	}

	public void addToFeed(Long userId, Long postId) {
		String key = "feed:" + userId;
		listOps.leftPush(key, postId);
		log.debug("Added post {} to feed for user {}", postId, userId);
	}

	public List<Long> getFeed(Long userId) {
		String key = "feed:" + userId;
		Long size = listOps.size(key);
		if (size == null || size == 0) {
			return Collections.emptyList();
		}
		return listOps.range(key, 0, -1);
	}

	public void clearFeed(Long userId) {
		String key = "feed:" + userId;
		redisTemplate.delete(key);
	}

	public int getFeedSize(Long userId) {
		String key = "feed:" + userId;
		Long size = listOps.size(key);
		return size != null ? size.intValue() : 0;
	}
}
