package com.cyan.example.application.user.cmd;

/**
 * 创建用户命令
 *
 * @author cy.Y
 * @since 1.0.0
 */
public record UserCmd(
        // 用户名
        String username,
        // 密码
        String password) {
}
