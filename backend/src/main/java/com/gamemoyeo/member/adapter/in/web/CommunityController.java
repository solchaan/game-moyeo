package com.gamemoyeo.member.adapter.in.web;

import com.gamemoyeo.common.exception.ApiException;
import com.gamemoyeo.member.application.CommunityUseCase;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CommunityController {
    private final CommunityUseCase useCase;
    public CommunityController(CommunityUseCase useCase) { this.useCase = useCase; }
    @GetMapping("/api/v1/members/me/companions")
    CommunityUseCase.CompanionPage companions(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) Long cursor,@RequestParam(defaultValue="20") int size) { return useCase.companions(member(jwt),cursor,size); }
    @GetMapping("/api/v1/members/me/blocks")
    CommunityUseCase.BlockPage blocks(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) Long cursor,@RequestParam(defaultValue="20") int size) { return useCase.blocks(member(jwt),cursor,size); }
    @PutMapping("/api/v1/members/me/blocks/{target}")
    ResponseEntity<Void> block(@AuthenticationPrincipal Jwt jwt,@PathVariable long target,@jakarta.validation.Valid @RequestBody Toggle request) {
        useCase.block(member(jwt),target,request.enabled());
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/api/v1/members/me/meetups/{meetup}/endorsements/{target}")
    ResponseEntity<Void> endorse(@AuthenticationPrincipal Jwt jwt,@PathVariable long meetup,@PathVariable long target,@jakarta.validation.Valid @RequestBody Toggle request) {
        useCase.endorse(member(jwt),target,meetup,request.enabled());
        return ResponseEntity.noContent().build();
    }
    @PutMapping("/api/v1/members/me/meetups/{meetup}/reports/{target}")
    ResponseEntity<Void> report(@AuthenticationPrincipal Jwt jwt,@PathVariable long meetup,@PathVariable long target,@RequestBody ReportRequest request) {
        useCase.report(member(jwt),target,meetup,request.reason(),request.details());
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/api/v1/admin/reports")
    CommunityUseCase.ReportPage reports(@RequestParam(required=false) Long cursor,@RequestParam(defaultValue="20") int size) { return useCase.reports(cursor,size); }
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/api/v1/admin/reports/{id}/resolution")
    ResponseEntity<Void> resolve(@AuthenticationPrincipal Jwt jwt,@PathVariable long id,@RequestBody Resolution request) {
        useCase.resolve(member(jwt),id,request.status(),request.resolution());
        return ResponseEntity.noContent().build();
    }
    record Toggle(@jakarta.validation.constraints.NotNull Boolean enabled) { }
    record ReportRequest(String reason,String details) { }
    record Resolution(String status,String resolution) { }
    private long member(Jwt jwt) {
        if (jwt==null) { throw new ApiException(HttpStatus.UNAUTHORIZED,"AUTHENTICATION_REQUIRED","Authentication is required."); }
        try { return Long.parseLong(jwt.getSubject()); }
        catch (NumberFormatException error) { throw new ApiException(HttpStatus.UNAUTHORIZED,"INVALID_AUTHENTICATION_SUBJECT","Invalid subject."); }
    }
}
