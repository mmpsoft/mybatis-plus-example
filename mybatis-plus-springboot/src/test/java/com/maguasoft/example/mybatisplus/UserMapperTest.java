package com.maguasoft.example.mybatisplus;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.segments.MergeSegments;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.maguasoft.example.mybatisplus.domain.UserEntity;
import com.maguasoft.example.mybatisplus.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@Slf4j
@SpringBootTest
public class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    //    @Test
    public void testSelect() {
        List<UserEntity> userList = userMapper.selectList(null);
        log.info("{}", userList);

        LambdaQueryWrapper<UserEntity> queryWrapper = Wrappers.lambdaQuery(UserEntity.class)
                .select(UserEntity::getId)
                .eq(UserEntity::getId, 4)
                .ge(UserEntity::getAge, 21)
                .likeLeft(UserEntity::getEmail, "@baomidou.com");
        UserEntity userEntity = userMapper.selectOne(queryWrapper);
        log.info("{}", userEntity);
        log.info("{}", queryWrapper.getSqlSelect());
        log.info("{}", queryWrapper.getSqlComment());
        log.info("{}", queryWrapper.getSqlFirst());
        log.info("{}", queryWrapper.getSqlSet());

        log.info("{}", queryWrapper.getSqlSegment());
        log.info("{}", queryWrapper.getCustomSqlSegment());
        log.info("{}", queryWrapper.getTargetSql());

        MergeSegments expression = queryWrapper.getExpression();
        log.info("{}", expression);

        log.info("{}", expression.getSqlSegment());
        log.info("{}", expression.getGroupBy());
        log.info("{}", expression.getNormal());
        log.info("{}", expression.getHaving());


//        Wrappers.lambdaQuery(UserEntity.class)
//                .eq(UserEntity::getName, "Tom")
//                .nested(w -> w.select(Order::getUserId).gt(Order::getTotalPrice, 1000), User::getId, Order::getUserId);
//
//        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
//        wrapper.lambda()
//                .eq(UserEntity::getName, "Tom")
//                .nested(w -> w.select(Order::getUserId)
//                                .gt(Order::getTotalPrice, 1000),
//                        User::getId, Order::getUserId);
    }

    @Test
    public void testSubSql() {
        log.info("{}", LambdaSubQueryWrapper.of(UserEntity.class)
                .select(UserEntity::getId)
                .eq(UserEntity::getId, 4)
                .ge(UserEntity::getAge, 21)
                .likeLeft(UserEntity::getEmail, "@baomidou.com")
//                .exists("select * from `user` where id > ?", 4)
                .getSql());

        LambdaQueryWrapper<UserEntity> queryWrapper = Wrappers.lambdaQuery(UserEntity.class)
                .eq(UserEntity::getName, "Jack")
                .geSql(UserEntity::getId, "select sub.id from `user` sub where (sub.id = 4 AND sub.age >= 21 AND sub.email LIKE '%@baomidou.com')");
        UserEntity userEntity = userMapper.selectOne(queryWrapper);
        log.info("{}", userEntity);
    }
}
