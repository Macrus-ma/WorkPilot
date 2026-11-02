package com.workpilot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("ticket_category")
public class TicketCategoryEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private Long responsibleDepartmentId;
    private String status;

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Long getResponsibleDepartmentId() {
        return responsibleDepartmentId;
    }
    public void setResponsibleDepartmentId(Long responsibleDepartmentId) {
        this.responsibleDepartmentId = responsibleDepartmentId;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}
