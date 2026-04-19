package com.cyan.example.domain.user.repository;

import com.cyan.arch.common.api.Page;
import com.cyan.example.domain.user.User;
import com.cyan.example.domain.user.query.UserListQuery;
import com.cyan.example.domain.user.query.UserPageQuery;

import java.util.List;

/**
 * 用户仓库
 * @author cy.Y
 * @since 1.0.0
 */
public interface UserRepository {

    /**
     * 根据id查询
     */
    User getById(String id);

    /**
     * 列表查询
     */
    List<User> list(UserListQuery query);

    /**
     * 分页查询
     */
    Page<User> page(UserPageQuery query);

    /**
     * 保存
     */
    User save(User user);

    /**
     * 修改
     */
    User update(User user);

    /**
     * 删除
     */
    void delete(String id);

    /**
     * 根据用户名查询
     */
    User getByUsername(String username);
}
