package edu.aiplatform.identity.controller;

import edu.aiplatform.identity.service.AccountAdminService;
import edu.aiplatform.identity.service.AccountImportService;
import edu.aiplatform.identity.service.AccountViews;
import edu.aiplatform.identity.contract.AccountStatus;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.model.AccountSearch;
import edu.aiplatform.shared.error.ApiException;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** UC 08–12 và nhập CSV (luồng phụ UC 09); chỉ ADMIN, kiểm ở service (BR-U01-67). */
@RestController
@RequestMapping("/api/v1/admin/accounts")
@Validated
public class AdminAccountController {

    private final AccountAdminService admin;
    private final AccountImportService imports;

    public AdminAccountController(AccountAdminService admin, AccountImportService imports) {
        this.admin = admin;
        this.imports = imports;
    }

    public static class CreateRequest extends StrictRequest {
        @NotNull
        @Size(max = 320)
        public String email;
        @NotNull
        @Size(max = 300)
        public String displayName;
        @NotNull
        public Role role;
    }

    /** Email, role, trạng thái không đổi ở đây (BR-U01-04; role, trạng thái có API riêng). */
    public static class UpdateRequest extends StrictRequest {
        @NotNull
        @Size(max = 300)
        public String displayName;
        @Size(max = 32)
        public String phoneNumber;
        @NotNull
        public Long version;
    }

    public static class RoleRequest extends StrictRequest {
        @NotNull
        public Role role;
        @NotNull
        public Long version;
    }

    public static class StatusRequest extends StrictRequest {
        @NotNull
        public AccountStatus status;
        @NotNull
        public Long version;
    }

    @GetMapping
    public AccountViews.Page list(@AuthenticationPrincipal Actor actor,
                                  @RequestParam(required = false) Role role,
                                  @RequestParam(required = false) AccountStatus status,
                                  @RequestParam(required = false) String q,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return admin.list(actor, new AccountSearch(role, status, q, page, size));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountViews.Detail create(@AuthenticationPrincipal Actor actor,
                                      @RequestBody @Validated CreateRequest request) {
        request.rejectUnknownFields();
        return admin.create(actor, request.email, request.displayName, request.role);
    }

    @GetMapping("/{id}")
    public AccountViews.Detail detail(@AuthenticationPrincipal Actor actor, @PathVariable UUID id) {
        return admin.detail(actor, id);
    }

    @PatchMapping("/{id}")
    public AccountViews.Detail update(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                      @RequestBody @Validated UpdateRequest request) {
        request.rejectUnknownFields();
        return admin.update(actor, id, request.displayName, request.phoneNumber, request.version);
    }

    @PutMapping("/{id}/role")
    public AccountViews.Detail changeRole(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                          @RequestBody @Validated RoleRequest request) {
        request.rejectUnknownFields();
        return admin.changeRole(actor, id, request.role, request.version);
    }

    @PutMapping("/{id}/status")
    public AccountViews.Detail changeStatus(@AuthenticationPrincipal Actor actor, @PathVariable UUID id,
                                            @RequestBody @Validated StatusRequest request) {
        request.rejectUnknownFields();
        return admin.changeStatus(actor, id, request.status, request.version);
    }

    @PostMapping("/imports:validate")
    public AccountImportService.ImportResult validateImport(@AuthenticationPrincipal Actor actor,
                                                            @RequestParam("file") MultipartFile file) {
        return imports.validate(actor, read(file));
    }

    @PostMapping("/imports:commit")
    public AccountImportService.ImportResult commitImport(@AuthenticationPrincipal Actor actor,
                                                          @RequestParam("file") MultipartFile file) {
        return imports.commit(actor, read(file));
    }

    /** Chỉ nhận tệp .csv, tối đa 1 MB (BR-U01-80). */
    private static byte[] read(MultipartFile file) {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);
        if (!name.endsWith(".csv")) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Chỉ nhận tệp .csv");
        }
        if (file.getSize() > AccountImportService.MAX_BYTES) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Tệp CSV vượt 1 MB");
        }
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("IMPORT_FILE_INVALID", "Không đọc được tệp CSV");
        }
    }
}
