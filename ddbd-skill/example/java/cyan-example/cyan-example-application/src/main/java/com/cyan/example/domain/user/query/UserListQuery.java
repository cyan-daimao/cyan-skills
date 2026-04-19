package com.cyan.example.domain.user.query;

/**
 * 用户列表查询
 * @author cy.Y
 * @since 1.0.0
 */
public record UserListQuery(
        // 用户id
        String id,
        // 用户名
        String username) {
}
