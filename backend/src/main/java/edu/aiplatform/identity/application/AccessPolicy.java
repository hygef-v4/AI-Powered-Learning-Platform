package edu.aiplatform.identity.application;

import static edu.aiplatform.identity.contract.Role.ADMIN;
import static edu.aiplatform.identity.contract.Role.STUDENT;
import static edu.aiplatform.identity.contract.Role.SUBJECT_MANAGER;
import static edu.aiplatform.identity.contract.Role.TEACHER;

import edu.aiplatform.identity.contract.Actions;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.identity.contract.ScopeKind;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Bảng quyền theo action (F13, BR-U01-60, permission matrix SRS 4.4). {@code scope} cho biết action cần kiểm phạm vi
 * gì: {@code NONE} chỉ kiểm role; {@code ACCOUNT} là dữ liệu của chính mình (R1); {@code SUBJECT}, {@code CLASS} hỏi
 * U04/U05. Action không có ở đây bị từ chối.
 */
final class AccessPolicy {

    record Rule(Set<Role> roles, ScopeKind scope) {
    }

    private static final Set<Role> ALL = EnumSet.allOf(Role.class);
    private static final Set<Role> ADMIN_ONLY = EnumSet.of(ADMIN);

    private static final Map<String, Rule> RULES = Map.ofEntries(
            Map.entry(Actions.ADMIN_DASHBOARD_VIEW, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.PROFILE_VIEW, new Rule(ALL, ScopeKind.ACCOUNT)),
            Map.entry(Actions.PROFILE_UPDATE, new Rule(ALL, ScopeKind.ACCOUNT)),
            Map.entry(Actions.PASSWORD_CHANGE, new Rule(ALL, ScopeKind.ACCOUNT)),
            Map.entry(Actions.ACCOUNT_LIST, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.ACCOUNT_VIEW, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.ACCOUNT_CREATE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.ACCOUNT_IMPORT, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.ACCOUNT_UPDATE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.ACCOUNT_ROLE_CHANGE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.ACCOUNT_STATUS_CHANGE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.AUDIT_READ, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            // BR-U03-04: Admin không upload; unit sở hữu purpose kiểm thêm phạm vi.
            Map.entry(Actions.FILE_UPLOAD, new Rule(EnumSet.of(STUDENT, TEACHER, SUBJECT_MANAGER), ScopeKind.NONE)),
            Map.entry(Actions.SETTINGS_VIEW, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SETTINGS_EDIT, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SUBJECT_LIST, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SUBJECT_VIEW, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SUBJECT_CREATE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SUBJECT_UPDATE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SUBJECT_MANAGER_CHANGE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.SUBJECT_STATUS_CHANGE, new Rule(ADMIN_ONLY, ScopeKind.NONE)),
            Map.entry(Actions.CLASS_STUDENT, new Rule(EnumSet.of(STUDENT), ScopeKind.NONE)),
            Map.entry(Actions.CLASS_TEACHER, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.NONE)),
            Map.entry(Actions.CLASS_MANAGER, new Rule(EnumSet.of(SUBJECT_MANAGER), ScopeKind.NONE)),
            Map.entry(Actions.CLASS_VIEW, new Rule(EnumSet.of(STUDENT, TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.CLASS_MANAGE, new Rule(EnumSet.of(SUBJECT_MANAGER), ScopeKind.SUBJECT)),
            Map.entry(Actions.GRADING_REVIEW, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.GRADING_WRITE, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.GRADEBOOK_READ, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.GRADEBOOK_EXPORT, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.MY_GRADE_READ, new Rule(EnumSet.of(STUDENT), ScopeKind.CLASS)),

            Map.entry(Actions.QUESTION_BANK_VIEW, new Rule(EnumSet.of(SUBJECT_MANAGER), ScopeKind.SUBJECT)),
            Map.entry(Actions.QUESTION_BANK_MANAGE, new Rule(EnumSet.of(SUBJECT_MANAGER), ScopeKind.SUBJECT)),
            Map.entry(Actions.QUIZ_SUBJECT_MANAGE, new Rule(EnumSet.of(SUBJECT_MANAGER), ScopeKind.SUBJECT)),
            Map.entry(Actions.QUIZ_CLASS_MANAGE, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.QUIZ_PRACTICE, new Rule(EnumSet.of(STUDENT), ScopeKind.CLASS)),
            Map.entry(Actions.GROUP_MANAGE, new Rule(EnumSet.of(TEACHER, SUBJECT_MANAGER), ScopeKind.CLASS)),
            Map.entry(Actions.GROUP_WORK, new Rule(EnumSet.of(STUDENT), ScopeKind.CLASS)));

    private AccessPolicy() {
    }

    static Optional<Rule> ruleFor(String action) {
        return Optional.ofNullable(action == null ? null : RULES.get(action));
    }
}
