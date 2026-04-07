package lk.ijse.examsybackend.service.impl;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lk.ijse.examsybackend.service.RateLimitingService;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitingServiceImpl implements RateLimitingService {

    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();

    // We use the IP address for auth routes, and username for logged-in routes
    @Override
    public Bucket resolveBucket(String key) {
        return userBuckets.computeIfAbsent(key, this::createNewBucket);
    }

    private Bucket createNewBucket(String key) {
        // Allows 5 requests maximum, refills 1 token every 10 seconds.
        Refill refill = Refill.intervally(1, Duration.ofSeconds(2));
        Bandwidth limit = Bandwidth.classic(10, refill);
        return Bucket.builder().addLimit(limit).build();
    }
}