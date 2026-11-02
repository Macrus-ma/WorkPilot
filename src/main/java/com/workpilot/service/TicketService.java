package com.workpilot.service;

import com.workpilot.dto.CreateTicketDTO;
import com.workpilot.vo.TicketCategoryVO;
import com.workpilot.vo.TicketVO;
import java.util.List;

public interface TicketService {
    TicketVO create(Long requesterId, String requestKey, CreateTicketDTO request);
    TicketVO getMine(Long requesterId, Long ticketId);
    List<TicketVO> listMine(Long requesterId, int page, int size);
    List<TicketCategoryVO> listCategories();
}
