package com.gamemoyeo.meetup.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.meetup.application.MeetupTemplateUseCase;
import java.time.Instant;
import java.net.URI;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MeetupTemplateController {
    private final MeetupTemplateUseCase useCase;
    public MeetupTemplateController(MeetupTemplateUseCase useCase) { this.useCase = useCase; }
    @PutMapping("/api/v1/members/me/templates/from/{meetupId}")
    TemplateId save(@AuthenticationPrincipal Jwt jwt,@PathVariable long meetupId) { return new TemplateId(useCase.save(member(jwt),meetupId)); }
    @GetMapping("/api/v1/members/me/templates")
    MeetupTemplateUseCase.Page find(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) Long cursor,@RequestParam(defaultValue="20") int size) {
        return useCase.find(member(jwt),cursor,size);
    }
    @DeleteMapping("/api/v1/members/me/templates/{id}")
    ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt,@PathVariable long id) {
        useCase.delete(member(jwt),id);
        return ResponseEntity.noContent().build();
    }
    @PostMapping("/api/v1/members/me/templates/{id}/meetups")
    ResponseEntity<MeetupTemplateUseCase.Created> create(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,
        @RequestHeader("Idempotency-Key") String key,@RequestBody Schedule request) {
        var created = useCase.create(member(jwt),id,request.startsAt(),request.weeks(),request.timeZone(),key);
        return ResponseEntity.created(URI.create("/api/v1/meetups/"+created.meetupIds().getFirst())).body(created);
    }
    record Schedule(Instant startsAt,int weeks,String timeZone) { }
    record TemplateId(long id) { }
    private long member(Jwt jwt) {
        if (jwt==null) { throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Authentication is required."); }
        try { return Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException error) { throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_AUTHENTICATION_SUBJECT","Invalid subject."); }
    }
}
