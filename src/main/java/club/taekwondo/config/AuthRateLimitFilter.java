package club.taekwondo.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class AuthRateLimitFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/utilisateurs/login";
    private static final String REGISTER_PATH = "/api/utilisateurs/register";
    private static final int CLEANUP_INTERVAL = 200;
    private static final Duration BUCKET_TTL = Duration.ofHours(6);

    private final Map<String, BucketHolder> buckets = new ConcurrentHashMap<>();
    private final AtomicInteger requestCounter = new AtomicInteger();

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        return !LOGIN_PATH.equals(path) && !REGISTER_PATH.equals(path);
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Instant now = Instant.now();
        cleanupIfNeeded(now);

        String path = request.getRequestURI();
        String clientKey = resolveClientKey(request);
        String bucketKey = path + ":" + clientKey;

        BucketHolder holder = buckets.compute(bucketKey, (key, existing) -> {
            if (existing == null) {
                return new BucketHolder(newBucketForPath(path), now);
            }
            existing.lastSeenAt = now;
            return existing;
        });

        ConsumptionProbe probe = holder.bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long retryAfterSeconds = Math.max(1L, (long) Math.ceil(probe.getNanosToWaitForRefill() / 1_000_000_000.0));
            response.setStatus(HttpServletResponse.SC_TOO_MANY_REQUESTS);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(
                    "{\"message\":\"Trop de tentatives d'authentification. Reessayez plus tard.\",\"retryAfterSeconds\":"
                            + retryAfterSeconds + "}"
            );
            return;
        }

        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
        filterChain.doFilter(request, response);
    }

    private Bucket newBucketForPath(String path) {
        if (REGISTER_PATH.equals(path)) {
            return Bucket.builder()
                    .addLimit(Bandwidth.classic(3, Refill.greedy(3, Duration.ofMinutes(10))))
                    .build();
        }

        return Bucket.builder()
                .addLimit(Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1))))
                .build();
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwardedFor)) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void cleanupIfNeeded(Instant now) {
        if (requestCounter.incrementAndGet() % CLEANUP_INTERVAL != 0) {
            return;
        }
        buckets.entrySet().removeIf(entry -> entry.getValue().lastSeenAt.plus(BUCKET_TTL).isBefore(now));
    }

    private static final class BucketHolder {
        private final Bucket bucket;
        private volatile Instant lastSeenAt;

        private BucketHolder(Bucket bucket, Instant lastSeenAt) {
            this.bucket = bucket;
            this.lastSeenAt = lastSeenAt;
        }
    }
}
