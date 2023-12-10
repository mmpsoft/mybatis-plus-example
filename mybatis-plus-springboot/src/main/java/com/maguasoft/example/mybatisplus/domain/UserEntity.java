package com.maguasoft.example.mybatisplus.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@TableName("`user`")
public class UserEntity {
    private Long id;
    private String name;
    private Integer age;
    private String email;
}
