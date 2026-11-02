package com.workpilot.controller;

import com.workpilot.common.ApiException;
import com.workpilot.dto.CreateTicketDTO;
import com.workpilot.security.CurrentUser;
import com.workpilot.service.TicketService;
import com.workpilot.vo.TicketCategoryVO;
import com.workpilot.vo.TicketVO;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@PreAuthorize("hasRole('CUSTOMER')")
public class TicketController {
    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping("/categories")
    public List<TicketCategoryVO> categories() {
        return ticketService.listCategories();
    }

    @PostMapping
    public ResponseEntity<TicketVO> create(@AuthenticationPrincipal CurrentUser user,
                                              @RequestHeader("Idempotency-Key") String requestKey,
                                              @Valid
                                              @RequestBody CreateTicketDTO request) {
        if (requestKey.isBlank() || requestKey.length() > 64) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Idempotency-Key 长度必须在 1 到 64 之间");
        }
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ticketService.create(user.id(), requestKey, request));
    }

    @GetMapping("/{ticketId}")
    public TicketVO get(@AuthenticationPrincipal CurrentUser user,
                          @PathVariable Long ticketId) {
        return ticketService.getMine(user.id(), ticketId);
    }

    @GetMapping("/mine")
    public List<TicketVO> mine(@AuthenticationPrincipal CurrentUser user,
                                 @RequestParam(defaultValue = "1") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return ticketService.listMine(user.id(), page, size);
    }
}
