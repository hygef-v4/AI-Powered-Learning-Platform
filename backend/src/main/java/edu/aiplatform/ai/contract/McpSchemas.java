package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Role;
import java.util.*;

/** JSON-schema fragments shared by domain tools, not business validation. */
public final class McpSchemas {
    private McpSchemas(){}
    public static final Set<Role> STAFF=Set.of(Role.TEACHER,Role.SUBJECT_MANAGER);
    public static final String UUID="{\"type\":\"string\",\"format\":\"uuid\"}";
    public static final String TEXT="{\"type\":\"string\",\"maxLength\":200}";
    public static final String PAGE="\"page\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":1000000},\"size\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}";
    public static final String SCOPE="\"classId\":"+UUID+",\"subjectId\":"+UUID;
    public static String schema(String properties,String...required) {
        return "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{"+properties+"},\"required\":["+
                String.join(",",Arrays.stream(required).map(s->"\""+s+"\"").toList())+"]}";
    }
    public static McpToolDefinition read(String name,String description,String properties,String...required){
        return new McpToolDefinition(name,description,schema(properties,required),McpToolKind.READ,STAFF);
    }
}
