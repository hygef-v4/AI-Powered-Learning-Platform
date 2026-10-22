package edu.aiplatform.groups.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import edu.aiplatform.groupdocs.application.GroupDocAccess;
import edu.aiplatform.groupdocs.application.GroupSectionService;
import edu.aiplatform.groupdocs.application.GroupSectionService.SectionView;
import edu.aiplatform.groupdocs.application.GroupSubmissionService;
import edu.aiplatform.groupdocs.application.GroupWorkQueryService;
import edu.aiplatform.groupdocs.application.GroupWorkspaceService;
import edu.aiplatform.groupdocs.application.IGroupDocStreams;
import edu.aiplatform.groupdocs.domain.GroupSection;
import edu.aiplatform.groupdocs.web.GroupDocController;
import edu.aiplatform.groups.application.ClassGroupService;
import edu.aiplatform.groups.application.ClassGroupService.GroupSetView;
import edu.aiplatform.groups.application.GroupSetInvalidException;
import edu.aiplatform.groups.application.LeaderRequestService;
import edu.aiplatform.groups.application.MyGroupService;
import edu.aiplatform.groups.domain.GroupSetRules.Issue;
import edu.aiplatform.identity.contract.Actor;
import edu.aiplatform.identity.contract.Role;
import edu.aiplatform.shared.error.ApiException;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Mapping và tuần tự hóa của API U12; quyền thật nằm ở service (đã có unit test riêng). */
class GroupControllersTest {

    private final Actor actor = new Actor(UUID.randomUUID(), Role.TEACHER);
    private final ClassGroupService classGroups = mock(ClassGroupService.class);
    private final LeaderRequestService requests = mock(LeaderRequestService.class);
    private final MyGroupService myGroups = mock(MyGroupService.class);
    private final GroupWorkQueryService work = mock(GroupWorkQueryService.class);
    private final GroupWorkspaceService workspace = mock(GroupWorkspaceService.class);
    private final GroupSectionService sections = mock(GroupSectionService.class);
    private final GroupSubmissionService submissions = mock(GroupSubmissionService.class);
    private final GroupDocAccess docAccess = mock(GroupDocAccess.class);
    private final IGroupDocStreams streams = mock(IGroupDocStreams.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null));
        mvc = MockMvcBuilders.standaloneSetup(new GroupController(classGroups, requests, myGroups, work),
                        new GroupDocController(workspace, sections, submissions, work, docAccess, streams))
                .setControllerAdvice(new GroupExceptionHandler(), new GlobalExceptionHandler())
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).build();
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void saveGroupSetPassesDraftsAndReturnsIssuesWhenInvalid() throws Exception {
        UUID classId = UUID.randomUUID();
        UUID leader = UUID.randomUUID();
        when(classGroups.save(eq(actor), eq(classId), any())).thenReturn(
                new GroupSetView(classId, "OPEN", 4, List.of(), List.of(), List.of()));
        String body = "{\"version\":3,\"groups\":[{\"key\":\"new-1\",\"name\":\"Nhóm 1\",\"leaderId\":\"" + leader
                + "\",\"memberIds\":[\"" + leader + "\"]}]}";

        mvc.perform(put("/api/v1/classes/" + classId + "/groups").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.version").value(4));
        verify(classGroups).save(eq(actor), eq(classId), eq(new ClassGroupService.SaveCommand(3L,
                List.of(new edu.aiplatform.groups.domain.GroupSetRules.Draft("new-1", null, "Nhóm 1", leader, List.of(leader))))));

        when(classGroups.save(eq(actor), eq(classId), any())).thenThrow(new GroupSetInvalidException(
                List.of(new Issue("new-1", "GROUP_NAME_DUPLICATE", "Tên nhóm bị trùng"))));
        mvc.perform(put("/api/v1/classes/" + classId + "/groups").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("GROUP_SET_INVALID"))
                .andExpect(jsonPath("$.issues[0].groupKey").value("new-1"))
                .andExpect(jsonPath("$.issues[0].code").value("GROUP_NAME_DUPLICATE"));
    }

    @Test
    void colonActionsAndLeaderRequestsAreMapped() throws Exception {
        UUID classId = UUID.randomUUID();
        UUID requestId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();
        mvc.perform(post("/api/v1/classes/" + classId + "/groups:random-preview").contentType(MediaType.APPLICATION_JSON)
                .content("{\"maxSize\":4}")).andExpect(status().isOk());
        verify(classGroups).randomPreview(actor, classId, 4);
        mvc.perform(post("/api/v1/leader-requests/" + requestId + ":approve")).andExpect(status().isOk());
        verify(requests).approve(actor, requestId, null);
        mvc.perform(post("/api/v1/leader-requests/" + requestId + ":reject").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Nhóm ổn định\"}")).andExpect(status().isOk());
        verify(requests).reject(actor, requestId, "Nhóm ổn định");
        mvc.perform(post("/api/v1/groups/" + groupId + "/leader-requests").contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"Lý do đủ mười ký tự\"}")).andExpect(status().isCreated());
        verify(requests).submit(actor, groupId, "Lý do đủ mười ký tự", null);
    }

    @Test
    void sectionActionsDraftAndOutOfScopeAreMapped() throws Exception {
        UUID doc = UUID.randomUUID();
        UUID section = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        when(sections.claim(actor, doc, section)).thenReturn(new SectionView(section, "p1", "Mục 1", 1,
                GroupSection.Status.CLAIMED, actor.accountId(), actor.accountId(), null, null, null, null, 1));

        mvc.perform(post("/api/v1/group-docs/" + doc + "/sections/" + section + ":claim")).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLAIMED"));
        mvc.perform(post("/api/v1/group-docs/" + doc + "/sections/" + section + ":assign")
                .contentType(MediaType.APPLICATION_JSON).content("{\"assigneeId\":\"" + assignee + "\",\"replaceHolder\":true}"))
                .andExpect(status().isOk());
        verify(sections).assign(actor, doc, section, assignee, true);
        mvc.perform(put("/api/v1/group-docs/" + doc + "/sections/" + section + "/draft")
                .contentType(MediaType.APPLICATION_JSON).content("{\"version\":3,\"blocks\":[]}")).andExpect(status().isOk());
        verify(sections).saveDraft(actor, doc, section, 3L, List.of());

        when(workspace.workspace(actor, doc)).thenThrow(ApiException.notFound("GROUP_NOT_FOUND"));
        mvc.perform(get("/api/v1/group-docs/" + doc)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
    }

    @Test
    void eventStreamChecksAccessAndDisablesProxyBuffering() throws Exception {
        UUID doc = UUID.randomUUID();
        when(streams.open(doc, actor.accountId())).thenReturn(new SseEmitter());

        mvc.perform(get("/api/v1/group-docs/" + doc + "/events")).andExpect(status().isOk())
                .andExpect(header().string("X-Accel-Buffering", "no"));
        verify(docAccess).read(actor, doc);
    }

    @Test
    void submitAndExportAreMapped() throws Exception {
        UUID doc = UUID.randomUUID();
        when(submissions.export(actor, doc, true)).thenReturn(new GroupSubmissionService.Export("Bài nhóm - Nhóm 1",
                new byte[] {1, 2}));
        mvc.perform(post("/api/v1/group-docs/" + doc + "/submit").contentType(MediaType.APPLICATION_JSON)
                .content("{\"version\":7}")).andExpect(status().isOk());
        verify(submissions).submit(actor, doc, 7L);
        mvc.perform(get("/api/v1/group-docs/" + doc + "/export.docx").param("submitted", "true"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
    }
}
