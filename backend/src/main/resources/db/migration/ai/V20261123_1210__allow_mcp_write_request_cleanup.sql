-- Job dọn định kỳ xóa bản ghi chống ghi trùng MCP cũ hơn 7 ngày; quyền runtime cần thêm delete.
do $$ begin
    if exists (select 1 from pg_roles where rolname = 'app') then
        grant delete on mcp_write_requests to app;
    end if;
end $$;
