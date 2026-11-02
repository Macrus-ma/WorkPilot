package com.workpilot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workpilot.common.ApiException;
import com.workpilot.dto.CreateTicketDTO;
import com.workpilot.entity.OperationRequestEntity;
import com.workpilot.entity.TicketActionLogEntity;
import com.workpilot.entity.TicketCategoryEntity;
import com.workpilot.entity.TicketEntity;
import com.workpilot.mapper.OperationRequestMapper;
import com.workpilot.mapper.TicketActionLogMapper;
import com.workpilot.mapper.TicketCategoryMapper;
import com.workpilot.mapper.TicketMapper;
import com.workpilot.service.TicketService;
import com.workpilot.vo.TicketCategoryVO;
import com.workpilot.vo.TicketVO;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketServiceImpl implements TicketService {
    private final TransactionTemplate transactionTemplate;
    private final TicketMapper ticketMapper;
    private final TicketCategoryMapper categoryMapper;
    private final TicketActionLogMapper logMapper;
    private final OperationRequestMapper operationMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock = Clock.systemUTC();

    public TicketServiceImpl(PlatformTransactionManager transactionManager,
                             TicketMapper ticketMapper, TicketCategoryMapper categoryMapper,
                             TicketActionLogMapper logMapper, OperationRequestMapper operationMapper,
                             ObjectMapper objectMapper) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.ticketMapper = ticketMapper;
        this.categoryMapper = categoryMapper;
        this.logMapper = logMapper;
        this.operationMapper = operationMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TicketVO create(Long requesterId, String requestKey, CreateTicketDTO request) {
        String requestHash = hash(request);
        try {
            Long ticketId = transactionTemplate.execute(status ->
                    writeNewTicket(requesterId, requestKey, requestHash, request));
            return getMine(requesterId, ticketId);
        } catch (DuplicateKeyException exception) {
            OperationRequestEntity existing = operationMapper.selectOne(
                    Wrappers.<OperationRequestEntity>lambdaQuery()
                            .eq(OperationRequestEntity::getActorId, requesterId)
                            .eq(OperationRequestEntity::getOperationType, "CREATE_TICKET")
                            .eq(OperationRequestEntity::getRequestKey, requestKey));
            if (existing == null) {
                throw exception;
            }
            if (!existing.getRequestHash().equals(requestHash)) {
                throw new ApiException(HttpStatus.CONFLICT, "同一请求键对应不同工单内容");
            }
            return getMine(requesterId, existing.getResourceId());
        }
    }

    @Override
    public TicketVO getMine(Long requesterId, Long ticketId) {
        TicketEntity ticket = ticketMapper.selectOwned(ticketId, requesterId);
        if (ticket == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "工单不存在");
        }
        return toTicketVO(ticket);
    }

    @Override
    public List<TicketVO> listMine(Long requesterId, int page, int size) {
        if (page < 1 || size < 1 || size > 50) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "page 必须大于 0，size 必须在 1 到 50 之间");
        }
        long offset = (long) (page - 1) * size;
        return ticketMapper.selectMine(requesterId, size, offset).stream()
                .map(this::toTicketVO)
                .toList();
    }

    @Override
    public List<TicketCategoryVO> listCategories() {
        return categoryMapper.selectList(Wrappers.<TicketCategoryEntity>lambdaQuery()
                        .eq(TicketCategoryEntity::getStatus, "ACTIVE")
                        .orderByAsc(TicketCategoryEntity::getId)).stream()
                .map(this::toCategoryVO)
                .toList();
    }

    private TicketVO toTicketVO(TicketEntity ticket) {
        return new TicketVO(ticket.getId(), ticket.getCategoryId(), ticket.getTitle(),
                ticket.getDescription(), ticket.getPriority(), ticket.getStatus(),
                ticket.getDueAt(), ticket.getCreatedAt());
    }

    private TicketCategoryVO toCategoryVO(TicketCategoryEntity category) {
        return new TicketCategoryVO(category.getId(), category.getName());
    }

    private Long writeNewTicket(Long requesterId, String requestKey, String requestHash,
                                CreateTicketDTO request) {
        OperationRequestEntity operation = new OperationRequestEntity();
        operation.setActorId(requesterId);
        operation.setOperationType("CREATE_TICKET");
        operation.setRequestKey(requestKey);
        operation.setRequestHash(requestHash);
        operationMapper.insert(operation);

        TicketCategoryEntity category = categoryMapper.selectById(request.categoryId());
        if (category == null || !"ACTIVE".equals(category.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "工单分类不存在或已停用");
        }

        TicketEntity ticket = new TicketEntity();
        ticket.setRequesterId(requesterId);
        ticket.setDepartmentId(category.getResponsibleDepartmentId());
        ticket.setCategoryId(category.getId());
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setStatus("NEW");
        ticket.setVersion(0);
        long hours = switch (request.priority()) {
            case "P1" -> 4;
            case "P2" -> 24;
            default -> 72;
        };
        ticket.setDueAt(LocalDateTime.now(clock).plusHours(hours));
        ticketMapper.insert(ticket);

        TicketActionLogEntity log = new TicketActionLogEntity();
        log.setTicketId(ticket.getId());
        log.setOperatorId(requesterId);
        log.setAction("CREATE");
        log.setToStatus("NEW");
        logMapper.insert(log);

        operation.setResourceId(ticket.getId());
        operationMapper.updateById(operation);
        return ticket.getId();
    }

    private String hash(CreateTicketDTO request) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(request);
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(json);
            return HexFormat.of().formatHex(digest);
        } catch (JsonProcessingException | java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("无法计算请求摘要", exception);
        }
    }
}
