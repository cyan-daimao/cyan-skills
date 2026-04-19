package com.cyan.example.infra.persistence.user.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cyan.arch.common.api.Page;
import com.cyan.arch.common.util.Convert;
import com.cyan.arch.common.util.StrUtils;
import com.cyan.example.domain.user.User;
import com.cyan.example.domain.user.query.UserListQuery;
import com.cyan.example.domain.user.query.UserPageQuery;
import com.cyan.example.domain.user.repository.UserRepository;
import com.cyan.example.infra.persistence.user.convert.UserInfraConvert;
import com.cyan.example.infra.persistence.user.dos.UserDO;
import com.cyan.example.infra.persistence.user.mappers.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户仓储实现
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;
    private final UserInfraConvert userInfraConvert;

    /**
     * 根据id查询
     */
    @Override
    public User getById(String id) {
        UserDO userDO = userMapper.selectById(id);
        return userInfraConvert.toUser(userDO);
    }

    /**
     * 列表查询
     *
     */
    @Override
    public List<User> list(UserListQuery query) {
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<UserDO>()
                .eq(StrUtils.isNotBlank(query.id()), UserDO::getId, query.id())
                .eq(StrUtils.isNotBlank(query.username()), UserDO::getUsername, query.username());
        List<UserDO> userDOS = userMapper.selectList(queryWrapper);
        return userInfraConvert.toUsers(userDOS);
    }

    /**
     * 分页查询
     *
     */
    @Override
    public Page<User> page(UserPageQuery query) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UserDO> page = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(query.current(), query.size());
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<UserDO>()
                .eq(StrUtils.isNotBlank(query.id()), UserDO::getId, query.id())
                .eq(StrUtils.isNotBlank(query.username()), UserDO::getUsername, query.username());
        page = userMapper.selectPage(page, queryWrapper);
        List<User> data = userInfraConvert.toUsers(page.getRecords());
        return new Page<>(data, page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 保存
     *
     */
    @Override
    public User save(User user) {
        UserDO userDO = userInfraConvert.toUserDO(user);
        userMapper.insert(userDO);
        return getById(Convert.toStr(userDO.getId()));
    }

    /**
     * 修改
     *
     */
    @Override
    public User update(User user) {
        UserDO userDO = userInfraConvert.toUserDO(user);
        userMapper.updateById(userDO);
        return getById(Convert.toStr(userDO.getId()));
    }

    /**
     * 删除
     *
     */
    @Override
    public void delete(String id) {
        userMapper.deleteById(id);
    }

    /**
     * 根据用户名查询
     */
    @Override
    public User getByUsername(String username) {
        LambdaQueryWrapper<UserDO> queryWrapper = new LambdaQueryWrapper<UserDO>()
                .eq(UserDO::getUsername, username);
        UserDO userDO = userMapper.selectOne(queryWrapper);
        return userInfraConvert.toUser(userDO);
    }
}
