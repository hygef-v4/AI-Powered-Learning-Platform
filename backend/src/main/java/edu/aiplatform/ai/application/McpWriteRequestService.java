package edu.aiplatform.ai.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.aiplatform.ai.contract.*;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.shared.error.ApiException;
import java.util.*;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** A transaction-scoped PostgreSQL lock serializes retries, including the first insert. */
@Service
public class McpWriteRequestService implements IMcpWriteRequest {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final TransactionTemplate transaction;
    public McpWriteRequestService(JdbcTemplate jdbc, ObjectMapper json, PlatformTransactionManager manager) {
        this.jdbc=jdbc; this.json=json; this.transaction=new TransactionTemplate(manager);
        this.transaction.setTimeout(30);
    }
    @Override public Map<String,Object> execute(Actor actor,String operation,String requestRef,String fingerprint,
                                               Supplier<Map<String,Object>> mutation) {
        McpInput.teaching(actor);
        if(operation==null||operation.isBlank()||operation.length()>100||requestRef==null||requestRef.isBlank()
                ||requestRef.length()>200||fingerprint==null||fingerprint.length()>10000)throw McpInput.invalid();
        return transaction.execute(status -> {
            String key=actor.accountId()+":"+operation+":"+requestRef;
            jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 0))", rs -> {}, key);
            var rows=jdbc.queryForList("select fingerprint, response::text as response from mcp_write_requests "
                    +"where account_id=? and operation=? and request_ref=?",actor.accountId(),operation,requestRef);
            if(!rows.isEmpty()) {
                var row=rows.get(0);
                if(!fingerprint.equals(row.get("fingerprint")))throw ApiException.conflict("MCP_REQUEST_CONFLICT",
                        "requestRef đã dùng với nội dung khác");
                try { return json.readValue((String)row.get("response"),new TypeReference<Map<String,Object>>(){}); }
                catch(Exception e){throw new IllegalStateException("Invalid stored MCP response",e);}
            }
            var response=mutation.get();
            try {
                String encoded=json.writeValueAsString(response);
                jdbc.update("insert into mcp_write_requests(account_id,operation,request_ref,fingerprint,response) "
                        +"values (?,?,?,?,?::jsonb)",actor.accountId(),operation,requestRef,fingerprint,encoded);
                // Return the same JSON types on initial creation and replay.
                return json.readValue(encoded,new TypeReference<Map<String,Object>>(){});
            } catch(com.fasterxml.jackson.core.JsonProcessingException e) {
                throw new IllegalStateException("Invalid MCP write response",e);
            }
        });
    }
}
