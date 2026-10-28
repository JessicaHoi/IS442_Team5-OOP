package is442t1.studybuddy.group;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import is442t1.studybuddy.ApiTestSupport;

class GroupAvailabilityApiTest extends ApiTestSupport {
    @Autowired private JdbcTemplate jdbc;
    private String leader;
    private String member;
    private String memberEmail;
    private String group;

    @BeforeEach
    void setupGroup() throws Exception {
        leader = login(createStudent("Availability Leader", "WHEN101"));
        memberEmail = createStudent("Availability Member", "WHEN101");
        member = login(memberEmail);
        group = createGroup();
        join(member);
    }

    @Test
    void defaultsFollowProfileButOverridesStaySeparateForEachGroupAndResetRestoresTheProfile() throws Exception {
        setProfile("MON", "19:00", "21:00");
        mockMvc.perform(as(member, get(path()))).andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("PROFILE")).andExpect(jsonPath("$.revision").isEmpty())
                .andExpect(jsonPath("$.availability[0].day").value("MON"));
        String firstGroup = group;
        group = createGroup();
        join(member);
        String otherGroup = group;
        group = firstGroup;
        String revision = save(member, slots("TUE", "14:00", "17:00"));
        setProfile("WED", "10:00", "12:00");
        mockMvc.perform(as(member, get(path()))).andExpect(jsonPath("$.source").value("GROUP"))
                .andExpect(jsonPath("$.availability[0].day").value("TUE"));
        group = otherGroup;
        mockMvc.perform(as(member, get(path()))).andExpect(jsonPath("$.source").value("PROFILE"))
                .andExpect(jsonPath("$.availability[0].day").value("WED"));
        group = firstGroup;
        mockMvc.perform(as(member, delete(path()).param("revision", revision))).andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("PROFILE")).andExpect(jsonPath("$.availability[0].day").value("WED"));
        mockMvc.perform(as(member, get("/api/students/me"))).andExpect(jsonPath("$.preferences[0].availability[0].day").value("WED"));
    }

    @Test
    void emptyOverrideMeansUnavailableAndOlderSessionsCannotOverwriteOrResetIt() throws Exception {
        setProfile("MON", "19:00", "21:00");
        String old = save(member, slots("TUE", "14:00", "17:00"));
        String current = save(member, "[]");
        assertThat(current).isNotEqualTo(old);
        mockMvc.perform(as(member, get(path()))).andExpect(jsonPath("$.source").value("GROUP"))
                .andExpect(jsonPath("$.availability").isEmpty());
        mockMvc.perform(as(member, put(path())).content(body(old, "[]"))).andExpect(status().isConflict());
        mockMvc.perform(as(member, delete(path()).param("revision", old))).andExpect(status().isConflict());
        mockMvc.perform(as(member, put(path())).content(body(null, "[]"))).andExpect(status().isConflict());
        mockMvc.perform(as(member, delete(path()).param("revision", current))).andExpect(status().isOk());
        String recreated = save(member, "[]");
        mockMvc.perform(as(member, put(path())).content(body(current, "[]"))).andExpect(status().isConflict());
        assertThat(recreated).isNotEqualTo(current);
    }

    @Test
    void heatmapCountsAndNamesChangeForEveryHalfHourAndIncludeTheLeader() throws Exception {
        save(leader, slots("TUE", "14:00", "18:00"));
        save(member, slots("TUE", "14:00", "18:00"));
        String third = login(createStudent("Third Available Member", "WHEN101"));
        join(third);
        save(third, slots("TUE", "14:00", "17:00"));
        for (int i = 0; i < 2; i++) {
            String token = login(createStudent("Early Available Member " + i, "WHEN101"));
            join(token);
            save(token, slots("TUE", "14:00", "15:00"));
        }
        String response = mockMvc.perform(as(member, get("/api/groups/" + group + "/best-times")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.memberCount").value(5))
                .andReturn().getResponse().getContentAsString();
        List<Map<String, Object>> cells = read(response, "$.heatmap");
        assertThat(cells).hasSize(224);
        assertCell(cells, "14:00", 5);
        assertCell(cells, "14:30", 5);
        assertCell(cells, "16:30", 3);
        assertCell(cells, "17:00", 2);
        assertCell(cells, "17:30", 2);
        assertCell(cells, "18:00", 0);
    }

    @Test
    void enforcesMembershipClosedGroupsAndSlotValidation() throws Exception {
        String outsider = login(createStudent("Availability Outsider", "WHEN101"));
        mockMvc.perform(as(outsider, get(path()))).andExpect(status().isForbidden());
        mockMvc.perform(as(outsider, put(path())).content(body(null, "[]"))).andExpect(status().isForbidden());
        mockMvc.perform(as(outsider, get("/api/groups/" + group + "/best-times"))).andExpect(status().isNotFound());
        for (String slots : List.of("null", "[null]", slots("BAD", "14:00", "15:00"), slots("TUE", "25:00", "26:00"),
                slots("TUE", "17:00", "14:00"), "[" + slot("TUE", "14:00", "16:00") + "," + slot("TUE", "15:00", "17:00") + "]")) {
            mockMvc.perform(as(member, put(path())).content(body(null, slots))).andExpect(status().isBadRequest());
        }
        String revision = save(member, "[]");
        mockMvc.perform(as(leader, post("/api/groups/" + group + "/close"))).andExpect(status().isOk());
        mockMvc.perform(as(member, get(path()))).andExpect(status().isOk());
        mockMvc.perform(as(member, get("/api/groups/" + group + "/best-times"))).andExpect(status().isOk());
        mockMvc.perform(as(member, put(path())).content(body(revision, "[]"))).andExpect(status().isConflict());
        mockMvc.perform(as(member, delete(path()).param("revision", revision))).andExpect(status().isConflict());
    }

    @Test
    void removesOverridesWithMembershipAndDoesNotRestoreThemWhenRejoining() throws Exception {
        save(member, slots("TUE", "14:00", "17:00"));
        mockMvc.perform(as(leader, delete("/api/groups/" + group + "/members/" + idOf(memberEmail))))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from group_member_availability where student_id = ?", Long.class, idOf(memberEmail))).isZero();
        join(member);
        mockMvc.perform(as(member, get(path()))).andExpect(jsonPath("$.source").value("PROFILE"));
    }

    @Test
    void deletingAnAccountAlsoDeletesAvailabilityWithoutDeletingTheGroup() throws Exception {
        save(member, slots("TUE", "14:00", "17:00"));
        String admin = login(createAdministrator());
        UUID deletedId = idOf(memberEmail);
        mockMvc.perform(as(admin, delete("/api/admin/users/" + idOf(memberEmail)))).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from group_member_availability where student_id = ?", Long.class, deletedId)).isZero();
        mockMvc.perform(as(leader, get("/api/groups/" + group + "/best-times")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.memberCount").value(1));
    }

    private void assertCell(List<Map<String, Object>> cells, String start, int count) {
        var cell = cells.stream().filter(item -> item.get("day").equals("TUESDAY") && item.get("startTime").toString().startsWith(start)).findFirst().orElseThrow();
        assertThat(((Number) cell.get("freeCount")).intValue()).isEqualTo(count);
        assertThat((List<?>) cell.get("availableMembers")).hasSize(count);
    }

    private String save(String token, String slots) throws Exception {
        String current = mockMvc.perform(as(token, get(path()))).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String response = mockMvc.perform(as(token, put(path())).content(body(read(current, "$.revision"), slots)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return read(response, "$.revision");
    }

    private void setProfile(String day, String start, String end) throws Exception {
        mockMvc.perform(as(member, put("/api/students/me/preferences")).content("""
                {"preferences":[{"course":"WHEN101","meetingMode":"ONLINE","groupFormat":"SMALL_GROUP",
                "goals":["EXAM_PREPARATION"],"availability":%s}]}
                """.formatted(slots(day, start, end)))).andExpect(status().isOk());
    }

    private String createGroup() throws Exception {
        return read(mockMvc.perform(as(leader, post("/api/groups")).content("""
                {"courseCode":"WHEN101","name":"When To Meet","description":"Availability test",
                 "goals":["EXAM_PREPARATION"],"meetingMode":"ONLINE","maxSize":8,
                 "availability":[{"day":"WED","start":"19:00","end":"21:00"}]}
                """)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.id");
    }

    private void join(String token) throws Exception {
        mockMvc.perform(as(token, post("/api/groups/" + group + "/join-requests")).content("{}"))
                .andExpect(status().isCreated());
        String request = mockMvc.perform(as(leader, get("/api/groups/" + group + "/join-requests")))
                .andReturn().getResponse().getContentAsString();
        mockMvc.perform(as(leader, post("/api/groups/" + group + "/join-requests/" + read(request, "$[0].id") + "/accept")))
                .andExpect(status().isOk());
    }

    private String path() { return "/api/groups/" + group + "/availability/me"; }
    private String body(String revision, String slots) { return "{\"revision\":" + (revision == null ? "null" : "\"" + revision + "\"") + ",\"availability\":" + slots + "}"; }
    private String slots(String day, String start, String end) { return "[" + slot(day, start, end) + "]"; }
    private String slot(String day, String start, String end) { return "{\"day\":\"" + day + "\",\"start\":\"" + start + "\",\"end\":\"" + end + "\"}"; }
}
