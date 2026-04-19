package com.cyan.example.domain.user;

import com.cyan.arch.common.api.Assert;
import com.cyan.arch.common.api.SilentException;
import com.cyan.example.domain.user.repository.UserRepository;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * 用户
 *
 * @author cy.Y
 * @since 1.0.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class User {
    /**
     * 主键id
     */
    private String id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 保存
     */
    public User save(UserRepository userRepository) {
        Assert.notBlank(username, new SilentException("用户名不能为空"));
        Assert.notBlank(password, new SilentException("用户名不能为空"));
        User one = userRepository.getByUsername(this.username);
        Assert.isNull(one, new SilentException("用户已存在"));

        return userRepository.save(this);
    }

    /**
     * 修改
     */
    public User update(UserRepository userRepository) {
        Assert.notBlank(username, new SilentException("用户名不能为空"));
        Assert.notBlank(password, new SilentException("用户名不能为空"));
        User one = userRepository.getByUsername(this.username);
        Assert.isFalse(one != null && !one.id.equals(this.id), new SilentException("用户已存在"));
        return userRepository.update(this);
    }

    /**
     * 删除
     */
    public void delete(UserRepository userRepository) {
        userRepository.delete(this.id);
    }
}
