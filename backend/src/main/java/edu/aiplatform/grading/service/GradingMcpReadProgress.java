package edu.aiplatform.grading.service;

import edu.aiplatform.shared.error.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/** Ephemeral served-page coverage, shared across backend instances; never stores submission content. */
@Component
public class GradingMcpReadProgress {
    static final int TTL_SECONDS=1800;
    static final DefaultRedisScript<Long> ADVANCE=new DefaultRedisScript<>("""
            local current = redis.call('GET', KEYS[1])
            local offset = tonumber(ARGV[1])
            if (not current and offset > 0) or (current and tonumber(current) < offset) then return -1 end
            local next = math.max(tonumber(current) or 0, tonumber(ARGV[2]))
            redis.call('SET', KEYS[1], tostring(next), 'EX', ARGV[3])
            return next
            """,Long.class);
    private final StringRedisTemplate redis;
    public GradingMcpReadProgress(StringRedisTemplate redis){this.redis=redis;}

    public void advance(UUID actorId,UUID evaluationId,String fingerprint,int offset,int next) {
        try {
            Long result=redis.execute(ADVANCE,List.of(key(actorId,evaluationId,fingerprint)),
                    String.valueOf(offset),String.valueOf(next),String.valueOf(TTL_SECONDS));
            if(result==null)throw unavailable();
            if(result<0)throw required();
        } catch(DataAccessException ex){throw unavailable();}
    }

    public void requireComplete(UUID actorId,UUID evaluationId,String fingerprint,int totalBlocks) {
        try {
            String value=redis.opsForValue().get(key(actorId,evaluationId,fingerprint));
            if(value==null)throw required();
            if(Integer.parseInt(value)<totalBlocks)throw required();
        } catch(DataAccessException|NumberFormatException ex){throw unavailable();}
    }

    static String key(UUID actorId,UUID evaluationId,String fingerprint) {
        return "mcp:grading-read:"+actorId+":"+evaluationId+":"+fingerprint;
    }
    static ApiException required(){return ApiException.conflict("MCP_CONTEXT_READ_REQUIRED",
            "Read every grading context page from offset 0 with the current fingerprint, then retry");}
    private static ApiException unavailable(){return ApiException.unavailable("MCP_CONTEXT_READ_UNAVAILABLE",
            "Cannot verify grading context reads; retry after the service recovers");}
}
