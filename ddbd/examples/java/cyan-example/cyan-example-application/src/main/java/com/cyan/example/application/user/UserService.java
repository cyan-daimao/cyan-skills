package com.cyan.example.application.user;

import com.cyan.arch.common.api.Page;
import com.cyan.example.application.user.bo.UserBO;
import com.cyan.example.application.user.cmd.UserCmd;
import com.cyan.example.domain.user.query.UserPageQuery;

/**
 * 用户服务
 *
 * @author cy.Y
 * @since 1.0.0
 */
public interface UserService {
    /**
     * 根据id查询用户
     */
    UserBO getById(String id);

    /**
     * 分页查询用户
     */
    Page<UserBO> page(UserPageQuery query);

    /**
     * 创建用户
     */
    UserBO save(UserCmd cmd);

    /**
     * 更新用户
     */
    UserBO update(String id,UserCmd cmd);

    /**
     * 删除用户
     */
    void delete(String id);
}
