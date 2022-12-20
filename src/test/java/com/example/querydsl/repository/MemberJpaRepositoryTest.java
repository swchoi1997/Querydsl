package com.example.querydsl.repository;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import com.example.querydsl.dto.MemberSearchCondition;
import com.example.querydsl.dto.MemberTeamDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.Team;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    public void searchTest(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(45);
        condition.setTeamName("teamB");

        //전부 다 null이 될때 문제된다. 동적쿼리 짤때는 리미트가 있던가, 기본값이 있는게 좋음 가급적이면 페이징쿼리가 들어가야

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }

    @Test
    public void searchTest2(){
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(35);
        condition.setAgeLoe(45);
        condition.setTeamName("teamB");

        //전부 다 null이 될때 문제된다. 동적쿼리 짤때는 리미트가 있던가, 기본값이 있는게 좋음 가급적이면 페이징쿼리가 들어가야함

        List<MemberTeamDto> result = memberJpaRepository.searchByWhere(condition);

        assertThat(result).extracting("username").containsExactly("member4");
    }
}