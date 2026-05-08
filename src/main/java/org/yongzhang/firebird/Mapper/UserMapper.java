package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.yongzhang.firebird.Data.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User getById(Long id);

    // ✅ 根据用户名和密码查询（登录用）
    @Select("SELECT * FROM user WHERE username = #{username} AND password = #{password}")
    User login(String username, String password);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO user(username, password) VALUES(#{username}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);
}