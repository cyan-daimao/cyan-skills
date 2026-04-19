package com.cyan.example.adapter.user.http;

import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.Response;
import com.cyan.example.adapter.user.convert.UserAdapterConvert;
import com.cyan.example.adapter.user.dto.UserDTO;
import com.cyan.example.application.user.UserService;
import com.cyan.example.application.user.bo.UserBO;
import com.cyan.example.application.user.cmd.UserCmd;
import com.cyan.example.domain.user.query.UserPageQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *
 * @author cy.Y
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserAdapterConvert userAdapterConvert;

    /**
     * 分页查询
     */
    @GetMapping
    public Response<Page<UserDTO>> page(UserPageQuery query) {
        Page<UserBO> page = userService.page(query);
        List<UserDTO> data = userAdapterConvert.toUserDTOList(page.getData());
        return Response.success(new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal()));
    }

    /**
     * 根据用户名查询
     */
    @GetMapping("/{id}")
    public Response<UserDTO> getById(@PathVariable("id") String id) {
        UserBO userBO = userService.getById(id);
        return Response.success(userAdapterConvert.toUserDTO(userBO));
    }

    /**
     * 保存用户
     */
    @PostMapping
    public Response<UserDTO> save(@RequestBody UserCmd cmd) {
        UserBO user = userService.save(cmd);
        return Response.success(userAdapterConvert.toUserDTO(user));
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Response<UserDTO> update(@PathVariable("id") String id, @RequestBody UserCmd cmd) {
        UserBO user = userService.update(id, cmd);
        return Response.success(userAdapterConvert.toUserDTO(user));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Response<Void> delete(@PathVariable("id") String id) {
        userService.delete(id);
        return Response.success();
    }
}
