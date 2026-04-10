package com.example.devflow.ratelimit;

import java.time.Instant;

public class TokenBucket {

    private final int capacity; // max tokens in the bucket
    private final double refillRate; // tokens per second
    private double tokens; // current token count
    private Instant lastRefillTime; // when we last refilled

    public TokenBucket(int capacity, double refillRate) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.tokens = capacity;
        this.lastRefillTime = Instant.now();
    }

    // Returns true if request is allowed, false if bucket is empty
    public synchronized boolean tryConsume() {
        refill();

        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }

        return false;
    }

    // How many seconds until next token is available
    public synchronized long getSecondsUntilRefill() {
        double tokensNeeded = 1 - tokens;
        return (long) Math.ceil(tokensNeeded / refillRate);
    }

    private void refill() {
        Instant now = Instant.now();
        double secondsElapsed = (now.toEpochMilli() - lastRefillTime.toEpochMilli()) / 1000.0;
        double tokensToAdd = secondsElapsed * refillRate;

        if (tokensToAdd > 0) {
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
    }

}
