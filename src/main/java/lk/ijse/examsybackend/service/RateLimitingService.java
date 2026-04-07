package lk.ijse.examsybackend.service;

import io.github.bucket4j.Bucket;

public interface RateLimitingService {
    // We use the IP address for auth routes, and username for logged-in routes
    Bucket resolveBucket(String key);
}
