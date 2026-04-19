package com.cyan.example.infra.persistence.user.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.example.domain.user.User;
import com.cyan.example.infra.persistence.user.dos.UserDO;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户仓储转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public abstract class UserInfraConvert {

    /**
     * 用户DO转换成用户
     */
    public abstract User toUser(UserDO userDO);

    /**
     * 用户DO列表转换成用户列表
     */
    public List<User> toUsers(List<UserDO> userDOs){
        return Optional.ofNullable(userDOs).orElse(List.of()).stream().map(this::toUser).collect(Collectors.toList());
    }

    /**
     * 用户转换成用户DO
     */
    public abstract UserDO toUserDO(User user);
}
