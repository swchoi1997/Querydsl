package com.example.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberDto2 {

    private String username;
    private int age;

    public MemberDto2() {
    }

    @QueryProjection
    public MemberDto2(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
