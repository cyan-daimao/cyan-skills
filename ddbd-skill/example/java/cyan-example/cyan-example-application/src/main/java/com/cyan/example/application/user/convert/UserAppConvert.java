package com.cyan.example.application.user.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.example.application.user.bo.UserBO;
import com.cyan.example.application.user.cmd.UserCmd;
import com.cyan.example.domain.user.User;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户业务转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public abstract class UserAppConvert {

    /**
     * 转换用户业务对象
     */
    public abstract UserBO toUserBO(User user);

    /**
     * 批量转换用户业务对象
     */
    public List<UserBO> toUserBOList(List<User> users) {
        return Optional.ofNullable(users).orElse(List.of()).stream().map(this::toUserBO).collect(Collectors.toList());
    }

    /**
     * 转换用户领域对象
     */
    public abstract User toUser(UserCmd cmd);


}
