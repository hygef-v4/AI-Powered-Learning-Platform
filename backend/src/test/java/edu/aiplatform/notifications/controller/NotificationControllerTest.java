package edu.aiplatform.notifications.controller;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import edu.aiplatform.announcements.controller.*;
import edu.aiplatform.announcements.dto.*;
import edu.aiplatform.announcements.service.*;
import edu.aiplatform.notifications.service.*;
import edu.aiplatform.identity.contract.*;
import edu.aiplatform.groupdocs.contract.ISseHub;
import edu.aiplatform.shared.error.GlobalExceptionHandler;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
class NotificationControllerTest {
    MockMvc mvc;NotificationService notices;AnnouncementService announcements;NotificationPreferenceService preferences;ISseHub sse;Actor actor=new Actor(UUID.randomUUID(),Role.STUDENT);
    @BeforeEach void setup(){notices=mock(NotificationService.class);announcements=mock(AnnouncementService.class);preferences=mock(NotificationPreferenceService.class);sse=mock(ISseHub.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor,null,List.of()));
        mvc=MockMvcBuilders.standaloneSetup(new NotificationController(notices,sse),new NotificationPreferenceController(preferences),new AnnouncementController(announcements),new MyAnnouncementController(announcements))
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver()).setControllerAdvice(new GlobalExceptionHandler(),new NotificationExceptionHandler()).build();}
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void countUsesAuthenticatedActor() throws Exception {when(notices.unread(actor)).thenReturn(4L);mvc.perform(get("/api/v1/me/notifications/unread-count")).andExpect(status().isOk()).andExpect(jsonPath("$.count").value(4));}
    @Test void readAllHasExplicitPostRoute() throws Exception {mvc.perform(post("/api/v1/me/notifications:read-all")).andExpect(status().isNoContent());verify(notices).readAll(actor);}
    @Test void foreignNotificationStays404() throws Exception {UUID id=UUID.randomUUID();doThrow(edu.aiplatform.shared.error.ApiException.notFound("NOTIFICATION_NOT_FOUND")).when(notices).read(actor,id);mvc.perform(post("/api/v1/me/notifications/"+id+":read")).andExpect(status().isNotFound());}
    @Test void sseUsesServerAccountChannelAndDisablesProxyBuffering() throws Exception {when(notices.authorizedChannel(actor)).thenReturn("notifications:account:"+actor.accountId());when(sse.subscribe(anyString())).thenReturn(new SseEmitter(1000L));mvc.perform(get("/api/v1/me/notifications/events").param("channel","attacker")).andExpect(status().isOk()).andExpect(header().string("X-Accel-Buffering","no"));verify(sse).subscribe("notifications:account:"+actor.accountId());}
    @Test void createRejectsBlankTitleBeforeService() throws Exception {mvc.perform(post("/api/v1/classes/"+UUID.randomUUID()+"/announcements").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"\",\"body\":\"b\"}")).andExpect(status().isBadRequest());verifyNoInteractions(announcements);}
    @Test void editRejectsNegativeVersion() throws Exception {mvc.perform(patch("/api/v1/announcements/"+UUID.randomUUID()).contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"t\",\"body\":\"b\",\"version\":-1}")).andExpect(status().isBadRequest());}
    @Test void preferenceBodyCallsOwnActorOnly() throws Exception {when(preferences.put(actor,Map.of("ENROLLED",false))).thenReturn(Map.of("ENROLLED",false));mvc.perform(put("/api/v1/me/notification-preferences").contentType(MediaType.APPLICATION_JSON).content("{\"ENROLLED\":false}")).andExpect(status().isOk()).andExpect(jsonPath("$.ENROLLED").value(false));}
}
