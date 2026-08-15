package com.example.common.dto;

public record TransferRequest(String fromCounter, String toCounter, long amount) {
}
