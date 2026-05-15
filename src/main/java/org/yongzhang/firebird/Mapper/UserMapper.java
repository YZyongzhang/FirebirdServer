package org.yongzhang.firebird.Mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.yongzhang.firebird.Data.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Delete;
import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM user WHERE id = #{id}")
    User getById(Long id);

    @Select("SELECT * FROM user WHERE id = #{id}")
    User findById(Long id);

    // ✅ 根据用户名和密码查询（登录用）
    @Select("SELECT * FROM user WHERE username = #{username} AND password = #{password}")
    User login(String username, String password);

    @Select("SELECT * FROM user WHERE username = #{username}")
    User findByUsername(String username);

    @Insert("INSERT INTO user(username, password, role) VALUES(#{username}, #{password}, #{role})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insertUser(User user);

    @Select("SELECT * FROM user")
    List<User> findAll();

    @Select("SELECT * FROM user WHERE role = #{role}")
    List<User> findByRole(String role);

    @Update("UPDATE user SET username = #{username}, role = #{role} WHERE id = #{id}")
    int updateUser(User user);

    @Delete("DELETE FROM user WHERE id = #{id}")
    int deleteUser(Long id);
}