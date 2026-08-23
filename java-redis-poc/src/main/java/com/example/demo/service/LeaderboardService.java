package com.example.demo.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import com.example.demo.dto.LeaderboardEntry;
import com.example.demo.exception.ResourceNotFoundException;

@Service
public class LeaderboardService {

    private static final String LEADERBOARD_KEY = "leaderboard";

    private final RedisTemplate<String, Object> redisTemplate;

    public LeaderboardService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void addScore(String player, double score) {
        redisTemplate.opsForZSet().add(LEADERBOARD_KEY, player, score);
    }

    public double incrementScore(String player, double increment) {
        Double newScore = redisTemplate.opsForZSet().incrementScore(LEADERBOARD_KEY, player, increment);
        return newScore != null ? newScore : 0.0;
    }

    public List<LeaderboardEntry> getTopN(int n) {
        Set<ZSetOperations.TypedTuple<Object>> tuples =
                redisTemplate.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, n - 1);
        List<LeaderboardEntry> entries = new ArrayList<>();
        if (tuples != null) {
            long rank = 1;
            for (ZSetOperations.TypedTuple<Object> tuple : tuples) {
                String player = tuple.getValue() != null ? tuple.getValue().toString() : "";
                double score = tuple.getScore() != null ? tuple.getScore() : 0.0;
                entries.add(new LeaderboardEntry(player, score, rank));
                rank++;
            }
        }
        return entries;
    }

    public LeaderboardEntry getPlayerRank(String player) {
        Long rank = redisTemplate.opsForZSet().reverseRank(LEADERBOARD_KEY, player);
        if (rank == null) {
            throw new ResourceNotFoundException("Player not found: " + player);
        }
        Double score = redisTemplate.opsForZSet().score(LEADERBOARD_KEY, player);
        return new LeaderboardEntry(player, score != null ? score : 0.0, rank + 1);
    }
}
