package lk.ijse.examsybackend.security;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lk.ijse.examsybackend.service.RateLimitingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    // 1. Define the endpoints you want to protect from spam
    private final List<String> protectedEndpoints = List.of(
            "/api/v1/auth/sign-in",
            "/api/v1/auth/forgot-password",
            "/api/v1/student/dashboard/classes/join"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // 2. Check if the current request is in our protected list
        boolean isProtected = protectedEndpoints.stream().anyMatch(requestURI::contains);

        if (isProtected) {
            String bucketKey;
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            // 3. Determine the Bucket Key (Who is making the request?)
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                // If logged in (e.g., joining a class), use their username
                bucketKey = auth.getName();
            } else {
                // If not logged in (e.g., login or forgot password), use their IP Address
                bucketKey = request.getRemoteAddr();
            }

            // 4. Get the bucket and try to consume a token
            Bucket tokenBucket = rateLimitingService.resolveBucket(bucketKey);

            if (!tokenBucket.tryConsume(1)) {
                // 5. BUCKET EMPTY - Block request
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.getWriter().write("{\"status\": 429, \"message\": \"Too many requests. Please wait before trying again.\"}");
                return; // Stop the request here
            }
        }

        // Allow request to proceed if it has a token or is not protected
        filterChain.doFilter(request, response);
    }
}