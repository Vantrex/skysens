package de.vantrex.skysens.server.controller;

import de.vantrex.skysens.common.api.NotificationPresetApi;
import de.vantrex.skysens.common.dto.common.PageDto;
import de.vantrex.skysens.common.dto.notification.NotificationPresetDto;
import de.vantrex.skysens.server.service.NotificationPresetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** §5.6 — shareable notification presets. */
@RestController
@RequestMapping("/api/v1/presets/notifications")
@RequiredArgsConstructor
public class NotificationPresetController implements NotificationPresetApi {

    private final NotificationPresetService notificationPresetService;

    @Override
    @GetMapping
    public PageDto<NotificationPresetDto> browse(
            @RequestParam(required = false) final String featureId,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size) {
        // TODO(deferred): paged list, optionally filtered by feature id
        throw new UnsupportedOperationException("TODO(deferred): preset browse");
    }

    @Override
    @PostMapping
    public NotificationPresetDto publish(@Valid @RequestBody final NotificationPresetDto preset) {
        // TODO(deferred): persist under the caller's UUID
        throw new UnsupportedOperationException("TODO(deferred): preset publish");
    }

    @Override
    @DeleteMapping("/{presetId}")
    public void delete(@PathVariable final UUID presetId) {
        // TODO(deferred): verify ownership, then delete
        throw new UnsupportedOperationException("TODO(deferred): preset delete");
    }
}
