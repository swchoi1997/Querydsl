package com.example.querydsl.repository;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.querydsl.entity.Member;
import java.util.List;
import javax.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest(){
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Member findMember = memberJpaRepository.findById(member1.getId()).get();
        assertThat(findMember).isEqualTo(member1);

        List<Member> result = memberJpaRepository.finaAll();
        assertThat(result).containsExactly(member1);

        List<Member> result2 = memberJpaRepository.findByUserName(member1.getUsername());
        assertThat(result2).containsExactly(member1);
    }

    @Test
    public void basicQueryDslTest(){
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);

        Member findMember = memberJpaRepository.findById(member1.getId()).get();
        assertThat(findMember).isEqualTo(member1);

        List<Member> result = memberJpaRepository.findAll_queryDsl();
        assertThat(result).containsExactly(member1);

        List<Member> result2 = memberJpaRepository.findByUsername_queryDsl(member1.getUsername());
        assertThat(result2).containsExactly(member1);
    }

}