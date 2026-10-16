package edu.aiplatform.identity.contract;

/**
 * Quyết định quyền phía server, mặc định từ chối: kiểm phiên, trạng thái {@code ACTIVE}, role,
 * rồi phạm vi môn hoặc lớp qua {@link ISubjectScope}, {@link IClassScope} (F13). Dùng bởi mọi unit và tool MCP.
 */
public interface IAuthorization {

    AuthorizationDecision authorize(Actor actor, String action, ResourceRef resourceRef);
}
