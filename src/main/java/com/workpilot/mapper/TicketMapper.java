package com.workpilot.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.workpilot.entity.TicketEntity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface TicketMapper extends BaseMapper<TicketEntity> {

    @Select("SELECT * FROM ticket WHERE requester_id = #{requesterId} "
            + "ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<TicketEntity> selectMine(@Param("requesterId") Long requesterId,
                                  @Param("limit") int limit,
                                  @Param("offset") long offset);

    @Select("SELECT * FROM ticket WHERE id = #{id} AND requester_id = #{requesterId}")
    TicketEntity selectOwned(@Param("id") Long id,
                             @Param("requesterId") Long requesterId);
}
