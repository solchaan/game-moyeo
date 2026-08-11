package com.gamemoyeo.game.adapter.in.web;

import com.gamemoyeo.game.application.GameCatalogUseCase;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameDetailView;
import com.gamemoyeo.game.application.GameCatalogUseCase.GameView;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionCommand;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionType;
import com.gamemoyeo.game.application.GameCatalogUseCase.OptionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class GameCatalogController {

    private final GameCatalogUseCase useCase;

    public GameCatalogController(GameCatalogUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping("/games")
    List<GameView> games() {
        return useCase.findActiveGames();
    }

    @GetMapping("/games/{gameId}")
    GameDetailView game(@PathVariable long gameId) {
        return useCase.findGame(gameId);
    }

    @PostMapping("/admin/games")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<GameDetailView> createGame(@Valid @RequestBody CreateGameRequest request) {
        GameDetailView created = useCase.createGame(request.gameCommand(), request.optionCommands());
        return ResponseEntity.created(URI.create("/api/v1/games/" + created.game().id())).body(created);
    }

    @PutMapping("/admin/games/{gameId}")
    @PreAuthorize("hasRole('ADMIN')")
    GameView updateGame(@PathVariable long gameId, @Valid @RequestBody GameRequest request) {
        return useCase.updateGame(gameId, request.command());
    }

    @DeleteMapping("/admin/games/{gameId}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deactivateGame(@PathVariable long gameId) {
        useCase.deactivateGame(gameId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/games/{gameId}/options")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<OptionView> createOption(
        @PathVariable long gameId,
        @Valid @RequestBody OptionRequest request
    ) {
        OptionView created = useCase.createOption(gameId, request.command());
        URI location = URI.create("/api/v1/games/" + gameId + "/options/" + created.id());
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/admin/games/{gameId}/options/{optionId}")
    @PreAuthorize("hasRole('ADMIN')")
    OptionView updateOption(
        @PathVariable long gameId,
        @PathVariable long optionId,
        @Valid @RequestBody OptionRequest request
    ) {
        return useCase.updateOption(gameId, optionId, request.command());
    }

    @DeleteMapping("/admin/games/{gameId}/options/{optionId}")
    @PreAuthorize("hasRole('ADMIN')")
    ResponseEntity<Void> deactivateOption(@PathVariable long gameId, @PathVariable long optionId) {
        useCase.deactivateOption(gameId, optionId);
        return ResponseEntity.noContent().build();
    }

    record GameRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") @Size(max = 80) String slug,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 2048) String imageUrl
    ) {
        GameCommand command() {
            return new GameCommand(slug, name, description, imageUrl);
        }
    }

    record CreateGameRequest(
        @NotBlank @Pattern(regexp = "^[a-z0-9-]+$") @Size(max = 80) String slug,
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @Size(max = 2048) String imageUrl,
        @Valid @Size(max = 200) List<OptionRequest> options
    ) {
        GameCommand gameCommand() {
            return new GameCommand(slug, name, description, imageUrl);
        }

        List<OptionCommand> optionCommands() {
            if (options == null) {
                return List.of();
            }
            return options.stream().map(OptionRequest::command).toList();
        }
    }

    record OptionRequest(
        @NotNull OptionType type,
        @NotBlank @Pattern(regexp = "^[A-Z0-9_]+$") @Size(max = 80) String code,
        @NotBlank @Size(max = 120) String displayName,
        @Max(100000) int sortOrder,
        @Size(max = 4000) String metadata
    ) {
        OptionCommand command() {
            return new OptionCommand(type, code, displayName, sortOrder, metadata);
        }
    }
}
