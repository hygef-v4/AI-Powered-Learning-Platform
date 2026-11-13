package edu.aiplatform.ai.contract;

import edu.aiplatform.identity.contract.*;
import edu.aiplatform.shared.error.ApiException;
import java.math.BigDecimal;
import java.util.*;

/** Shared bounded MCP input parsing; resource authorization remains with each domain. */
public final class McpInput {
    private final Map<String,Object> values;
    public McpInput(Map<String,Object> values) { this.values=values==null?Map.of():values; }
    public Object get(String key) { return values.get(key); }
    public boolean has(String key) { return values.containsKey(key); }
    public String text(String key,String fallback) {
        Object value=values.get(key);
        if(value==null)return fallback;
        if(!(value instanceof String s) || s.length()>10000)throw invalid();
        return s;
    }
    public UUID uuid(String key) {
        try { String s=text(key,null); if(s==null || !s.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}"))throw invalid(); return UUID.fromString(s); }
        catch(IllegalArgumentException e){throw invalid();}
    }
    public int integer(String key,int fallback,int min,int max) {
        Object value=values.get(key); if(value==null)return fallback;
        if(!(value instanceof Number))throw invalid();
        try {int number=new BigDecimal(value.toString()).intValueExact();if(number<min||number>max)throw invalid();return number;}
        catch(ArithmeticException|NumberFormatException e){throw invalid();}
    }
    public long version(String key) {
        Object value=values.get(key); if(!(value instanceof Number))throw invalid();
        try {long number=new BigDecimal(value.toString()).longValueExact();if(number<0)throw invalid();return number;}
        catch(ArithmeticException|NumberFormatException e){throw invalid();}
    }
    public <E extends Enum<E>> E choice(String key,Class<E> type,E fallback) {
        String value=text(key,null);if(value==null)return fallback;
        try{return Enum.valueOf(type,value);}catch(IllegalArgumentException e){throw invalid();}
    }
    public int page(){return integer("page",0,0,1_000_000);}
    public int size(){return integer("size",20,1,100);}
    public static <T> Map<String,Object> page(List<T> items,int page,int size) {
        long start=(long)page*size;int from=(int)Math.min(start,items.size());
        return Map.of("items",items.subList(from,Math.min(from+size,items.size())),"total",items.size(),"page",page,"size",size);
    }
    public static void teaching(Actor actor) {
        if(actor==null || (actor.role()!=Role.TEACHER && actor.role()!=Role.SUBJECT_MANAGER))throw ApiException.notFound("MCP_TOOL_NOT_FOUND");
    }
    public static void admin(Actor actor) {
        if(actor==null || actor.role()!=Role.ADMIN)throw ApiException.notFound("MCP_TOOL_NOT_FOUND");
    }
    public static ApiException invalid(){return ApiException.badRequest("INVALID_MCP_ARGUMENTS","Tham số MCP không hợp lệ");}
}
