package com.example.demo.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.LeaderboardEntry;
import com.example.demo.service.LeaderboardService;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @PostMapping
    public ResponseEntity<Void> addScore(@RequestParam String player, @RequestParam double score) {
        leaderboardService.addScore(player, score);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/increment")
    public ResponseEntity<Double> incrementScore(@RequestParam String player, @RequestParam double increment) {
        return ResponseEntity.ok(leaderboardService.incrementScore(player, increment));
    }

    @GetMapping("/top/{n}")
    public ResponseEntity<List<LeaderboardEntry>> getTopN(@PathVariable int n) {
        return ResponseEntity.ok(leaderboardService.getTopN(n));
    }

    @GetMapping("/rank/{player}")
    public ResponseEntity<LeaderboardEntry> getPlayerRank(@PathVariable String player) {
        return ResponseEntity.ok(leaderboardService.getPlayerRank(player));
    }
}
