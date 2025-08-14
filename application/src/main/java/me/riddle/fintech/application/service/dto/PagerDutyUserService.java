package me.riddle.fintech.application.service.dto;

import me.riddle.fintech.domain.model.dto.PagerDutyUser;

import java.util.Map;

/**
 * A stub for actual User, UserList, and UserListByFilter retrieval paging services.
 */
@SuppressWarnings("unused")
public class PagerDutyUserService {

    public PagerDutyUser getRemoteUser(String id) {
        return new PagerDutyUser(id, "Name", "email", "summary", "type", "self", "htmlUrl",
                "avatarUrl", "color", "role", "description", true, "jobTitle", "timeZone", Map.of());
    }

}
