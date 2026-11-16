// Test kiểm câu tiếng Việt: đặt cookie ngôn ngữ trước khi module nào đọc nó (ngôn ngữ mặc định của web là tiếng Anh).
if (typeof document !== "undefined") document.cookie = "locale=vi; path=/";
