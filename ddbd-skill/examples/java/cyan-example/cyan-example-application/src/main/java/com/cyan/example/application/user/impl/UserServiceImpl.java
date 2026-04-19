package com.cyan.example.application.user.impl;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.api.SilentException;
import com.cyan.example.application.user.UserService;
import com.cyan.example.application.user.bo.UserBO;
import com.cyan.example.application.user.cmd.UserCmd;
import com.cyan.example.application.user.convert.UserAppConvert;
import com.cyan.example.domain.user.User;
import com.cyan.example.domain.user.query.UserPageQuery;
import com.cyan.example.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户服务实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserAppConvert userAppConvert;


    /**
     * 根据id查询用户
     */
    @Override
    public UserBO getById(String id) {
        User user = userRepository.getById(id);
        return userAppConvert.toUserBO(user);
    }

    /**
     * 分页查询用户
     */
    @Override
    public Page<UserBO> page(UserPageQuery query) {
        Page<User> page = userRepository.page(query);
        List<UserBO> data = userAppConvert.toUserBOList(page.getData());
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 创建用户
     */
    @Override
    public UserBO save(UserCmd cmd) {
        User user = userAppConvert.toUser(cmd);
        user = user.save(userRepository);
        return userAppConvert.toUserBO(user);
    }

    /**
     * 更新用户
     */
    @Override
    public UserBO update(String id,UserCmd cmd) {
        User user = userAppConvert.toUser(cmd);
        user = user.update(userRepository);
        return userAppConvert.toUserBO(user);
    }

    /**
     * 删除用户
     */
    @Override
    public void delete(String id) {
        User user = userRepository.getById(id);
        Assert.notNull(user, new SilentException("用户不存在"));
        user.delete(userRepository);
    }
}
