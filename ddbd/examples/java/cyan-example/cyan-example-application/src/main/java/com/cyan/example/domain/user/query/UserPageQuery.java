package com.cyan.example.domain.user.query;

import com.cyan.arch.common.api.Pageable;

/**
 * 用户分页查询
 *
 * @author cy.Y
 * @since 1.0.0
 */
public record UserPageQuery(
        // 用户ID
        String id,
        // 用户名
        String username,
        // 当前页
        long current,
        // 每页大小
        long size
) implements Pageable {

}
