package com.cyan.example.adapter.user.convert;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import com.cyan.example.adapter.user.dto.UserDTO;
import com.cyan.example.application.user.bo.UserBO;
import org.mapstruct.Mapper;

import java.util.List;
import java.util.Optional;

/**
 * 用户适配器转换
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Mapper(componentModel = "spring", uses = MapstructConvert.class)
public abstract class UserAdapterConvert {

    /**
     * 转换为用户DTO
     */
    public abstract UserDTO toUserDTO(UserBO user);

    /**
     * 批量转换为用户DTO
     */
    public List<UserDTO> toUserDTOList(List<UserBO> users) {
        return Optional.of(users).orElse(List.of()).stream().map(this::toUserDTO).toList();
    }
}
