# U10 Template & Copy - Deployment Architecture

```
 Trình duyệt --HTTPS--> [nginx] --> [backend: U10 --gọi trong tiến trình--> U06, U08, U09]
                                          |
                                          v
                                    [postgres: template_releases; cột lineage của
                                               assignments, chính sách của publications]
```

**Text alternative**: Trình duyệt gọi qua Nginx tới module U10 trong backend. U10 gọi U06, U08, U09 trong cùng tiến trình và cùng transaction, lưu template và lineage vào PostgreSQL. Không có container hay volume mới.
