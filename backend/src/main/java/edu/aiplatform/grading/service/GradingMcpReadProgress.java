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
    private final StringRedisTemplate stringRedisTemplate;
    public GradingMcpReadProgress(StringRedisTemplate stringRedisTemplate){this.stringRedisTemplate=stringRedisTemplate;}

    public void advanceProgress(UUID actorId,UUID evaluationId,String fingerprint,int offset,int next) {
        try {
            Long result=stringRedisTemplate.execute(ADVANCE,List.of(buildProgressKey(actorId,evaluationId,fingerprint)),
                    String.valueOf(offset),String.valueOf(next),String.valueOf(TTL_SECONDS));
            if(result==null)throw createUnavailableError();
            if(result<0)throw createReadRequiredError();
        } catch(DataAccessException ex){throw createUnavailableError();}
    }

    public void requireCompleteRead(UUID actorId,UUID evaluationId,String fingerprint,int totalBlocks) {
        try {
            String value=stringRedisTemplate.opsForValue().get(buildProgressKey(actorId,evaluationId,fingerprint));
            if(value==null)throw createReadRequiredError();
            if(Integer.parseInt(value)<totalBlocks)throw createReadRequiredError();
        } catch(DataAccessException|NumberFormatException ex){throw createUnavailableError();}
    }

    static String buildProgressKey(UUID actorId,UUID evaluationId,String fingerprint) {
        return "mcp:grading-read:"+actorId+":"+evaluationId+":"+fingerprint;
    }
    static ApiException createReadRequiredError(){return ApiException.conflict("MCP_CONTEXT_READ_REQUIRED",
            "Read every grading context page from offset 0 with the current fingerprint, then retry");}
    private static ApiException createUnavailableError(){return ApiException.unavailable("MCP_CONTEXT_READ_UNAVAILABLE",
            "Cannot verify grading context reads; retry after the service recovers");}
}
