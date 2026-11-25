package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.Role;
import java.util.*;

/** JSON-schema fragments shared by domain tools, not business validation. */
public final class McpSchemas {
    // Ngăn tạo đối tượng cho lớp chỉ cung cấp các hàm tĩnh.
    private McpSchemas(){}
    public static final Set<Role> STAFF=Set.of(Role.TEACHER,Role.SUBJECT_MANAGER);
    public static final String UUID="{\"type\":\"string\",\"format\":\"uuid\"}";
    public static final String TEXT="{\"type\":\"string\",\"maxLength\":200}";
    public static final String PAGE="\"page\":{\"type\":\"integer\",\"minimum\":0,\"maximum\":1000000},\"size\":{\"type\":\"integer\",\"minimum\":1,\"maximum\":100}";
    public static final String SCOPE="\"classId\":"+UUID+",\"subjectId\":"+UUID;
    // Ghép các trường hợp lệ và bắt buộc vào schema object không nhận thuộc tính ngoài danh sách.
    public static String schema(String properties,String...required) {
        return "{\"type\":\"object\",\"additionalProperties\":false,\"properties\":{"+properties+"},\"required\":["+
                String.join(",",Arrays.stream(required).map(s->"\""+s+"\"").toList())+"]}";
    }
    // Tạo định nghĩa tool chỉ đọc dành cho vai trò staff từ phần khai báo tham số.
    public static McpToolDefinition read(String name,String description,String properties,String...required){
        return new McpToolDefinition(name,description,schema(properties,required),McpToolKind.READ,STAFF);
    }
}
