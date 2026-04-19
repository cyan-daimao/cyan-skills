package com.cyan.example.infra.persistence.user.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cyan.example.infra.persistence.user.dos.UserDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户Mapper接口
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper
public interface UserMapper extends BaseMapper<UserDO> {
}
