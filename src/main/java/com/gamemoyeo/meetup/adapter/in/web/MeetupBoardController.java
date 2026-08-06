package com.gamemoyeo.meetup.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.CursorPage;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupCommand;
import com.gamemoyeo.meetup.application.MeetupBoardUseCase.MeetupView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/meetups")
public class MeetupBoardController {

    private final MeetupBoardUseCase useCase;

    public MeetupBoardController(MeetupBoardUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    CursorPage findAll(
        @RequestParam(required = false) Long gameId,
        @RequestParam(required = false) Long cursor,
        @RequestParam(defaultValue = "20") int size
    ) {
        return useCase.findAll(gameId, cursor, size);
    }

    @GetMapping("/{meetupId}")
    MeetupView find(@PathVariable long meetupId) {
        return useCase.find(meetupId);
    }

    @PostMapping
    ResponseEntity<MeetupView> create(
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MeetupRequest request
    ) {
        MeetupView created = useCase.create(memberId(jwt), request.command());
        return ResponseEntity.created(URI.create("/api/v1/meetups/" + created.id())).body(created);
    }

    @PutMapping("/{meetupId}")
    MeetupView update(
        @PathVariable long meetupId,
        @AuthenticationPrincipal Jwt jwt,
        @Valid @RequestBody MeetupRequest request
    ) {
        return useCase.update(meetupId, memberId(jwt), request.command());
    }

    @DeleteMapping("/{meetupId}")
    ResponseEntity<Void> delete(@PathVariable long meetupId, @AuthenticationPrincipal Jwt jwt) {
        useCase.delete(meetupId, memberId(jwt));
        return ResponseEntity.noContent().build();
    }

    private long memberId(Jwt jwt) {
        if (jwt == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED", "Authentication is required.");
        }
        try {
            return Long.parseLong(jwt.getSubject());
        } catch (NumberFormatException exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "INVALID_AUTHENTICATION_SUBJECT",
                "Authentication subject is invalid.");
        }
    }

    record MeetupRequest(
        @Positive long gameId,
        @Positive Long modeOptionId,
        @Positive Long platformOptionId,
        @Positive Long regionOptionId,
        @Positive Long minimumTierOptionId,
        @Positive Long maximumTierOptionId,
        @NotBlank @Size(max = 160) String title,
        @Size(max = 10000) String description,
        @NotBlank @Pattern(regexp = "^(CASUAL|COMPETITIVE|PRACTICE|BEGINNER|SOCIAL)$") String playStyle,
        @NotBlank @Pattern(regexp = "^(REQUIRED|OPTIONAL|DISABLED)$") String voiceChatPolicy,
        @NotBlank @Pattern(regexp = "^(FIRST_COME|OWNER_APPROVAL)$") String approvalType,
        Instant recruitmentDeadline,
        @NotNull @Future Instant startsAt,
        @NotNull @Future Instant endsAt,
        @Positive @Max(100) int capacity,
        @Valid @Size(max = 20) List<RoleRequirementRequest> roleRequirements
    ) {
        MeetupCommand command() {
            Map<Long, Integer> roles = new HashMap<>();
            if (roleRequirements != null) {
                for (RoleRequirementRequest requirement : roleRequirements) {
                    if (roles.put(requirement.roleOptionId(), requirement.capacity()) != null) {
                        throw new ApiException(HttpStatus.BAD_REQUEST, "DUPLICATED_ROLE_REQUIREMENT",
                            "Role requirement cannot be duplicated.");
                    }
                }
            }
            return new MeetupCommand(gameId, modeOptionId, platformOptionId, regionOptionId,
                minimumTierOptionId, maximumTierOptionId, title, description, playStyle, voiceChatPolicy,
                approvalType, recruitmentDeadline, startsAt, endsAt, capacity, roles);
        }
    }

    record RoleRequirementRequest(@Positive long roleOptionId, @Positive @Max(100) int capacity) {
    }
}
