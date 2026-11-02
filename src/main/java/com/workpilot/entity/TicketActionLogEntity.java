package com.workpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ticket_action_log")
public class TicketActionLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long ticketId;
    private Long operatorId;
    private String action;
    private String toStatus;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getTicketId() {
        return ticketId;
    }
    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }
    public Long getOperatorId() {
        return operatorId;
    }
    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }
    public String getAction() {
        return action;
    }
    public void setAction(String action) {
        this.action = action;
    }
    public String getToStatus() {
        return toStatus;
    }
    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }
}
