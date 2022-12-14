package com.example.querydsl;

import static com.example.querydsl.entity.QMember.member;
import static com.example.querydsl.entity.QTeam.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.filter;

import com.example.querydsl.dto.MemberDto;
import com.example.querydsl.dto.MemberDto2;
import com.example.querydsl.dto.QMemberDto2;
import com.example.querydsl.dto.UserDto;
import com.example.querydsl.entity.Member;
import com.example.querydsl.entity.QMember;
import com.example.querydsl.entity.Team;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {
    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        queryFactory = new JPAQueryFactory(em);
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
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라
        Member findMember = em.createQuery(
                "select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQueryDsl(){
        QMember m = new QMember("m");
        Member findMember = queryFactory.select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //기본 QType 활용
    @Test
    public void startQueryDslDefault(){
        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //검색조건 쿼리
    @Test
    public void search1(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search2(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.between(10, 30)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), member.age.eq(10)
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //결과 조회
    @Test
    public void resultFetch(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .limit(1)
                .fetchFirst();
    }

    //정렬
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    //페이징
    @Test
    public void paging1(){
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(0) // 앞에서 몇개를 스킾할건지
                .limit(2)
                .fetch();
        assertThat(fetch.size()).isSameAs(2);
    }
    //집합

    @Test
    public void aggregation(){
        List<Tuple> fetch = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min())
                .from(member)
                .fetch();
        //-> 실무에서는 DTo로 뽑아오는 방식으로 사용한다.
        Tuple tuple = fetch.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    @Test
    public void having() throws Exception{
        List<Tuple> fetch = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = fetch.get(0);
        Tuple teamB = fetch.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    //join
    //첫번째 파라이터에 조인 대상을 지정하고, 두번째 파라미터에 별칭으로 사용할 Q타입을 지정한다.
    @Test
    public void defaultJoin() throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /*
    join on절
   1. 조인대상 필터링
   2. 연관관계 없는 엔티티 외부조인
     */
    @Test
    public void join_on_filtering() throws Exception{
        //회원과 팀을 조인하면서 팀이름이 teamA인 팀만 조인, 회원은 모두조회
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

        //inner join할 때는 on과 where join의 반환값이 같다 그러브로 그냥 where하자
    }

    @Test
    public void join_on_no_relation() throws Exception{
        //회원의이름과 팀의 이름이같은 사람 찾기
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> fetch1 = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) // 엔티티가 하나만 들어간다는 것을 주의할것!!
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch1) {
            System.out.println("tuple = " + tuple);
        }
    }

    //fetch join
    /*
    연관된 테이블을 한번에 조회하는 것이다!!
    주로 성능 최적화에서 사용하는 방법이다.
     */

    @PersistenceUnit
    EntityManagerFactory emf;
    @Test
    public void fetchJoinNo() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        //객체가 이미 로딩된 엔티티인지 아직 로딩안된 엔티티인지 알려줌
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoin() throws Exception{
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        //객체가 이미 로딩된 엔티티인지 아직 로딩안된 엔티티인지 알려줌
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    //subQuery

    @Test
    public void subQuery() throws Exception{
        //나이가 가장 많은 회원 조회

        QMember subMember = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions.select(subMember.age.max())
                                .from(subMember)
                ))
                .fetch();

        assertThat(fetch).extracting("age")
                .containsExactly(40);
    }

    @Test
    public void subQuery2() throws Exception{
        //나이가 평균 이상인 회원 조회

        QMember subMember = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions.select(subMember.age.avg())
                                .from(subMember)
                ))
                .fetch();

        assertThat(fetch).extracting("age")
                .containsExactly(30,40);
    }

    @Test
    public void subQuery_in() throws Exception{
        //나이가 평균 이상인 회원 조회

        QMember subMember = new QMember("memberSub");

        List<Member> fetch = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(subMember.age)
                                .from(subMember)
                                .where(member.age.gt(10))
                ))//효율 x 예제를 위해 사용
                .fetch();

        assertThat(fetch).extracting("age")
                .containsExactly(20, 30,40);
    }

    @Test
    public void selectSubQuery(){

        QMember subMember = new QMember("memberSub");
        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(subMember.age.avg())
                                .from(subMember)
                )
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    //case문

    @Test
    public void basicCase(){
        List<String> fetch = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("스무살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void complexCase(){
        List<String> etc = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("etc"))
                .from(member)
                .fetch();

        for (String s : etc) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void concat(){

        //enum조회 시 stringValue쓰임
        List<String> fetch = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection(){
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);
        }
    }

    @Test
    public void findDataJPQL(){
        List<MemberDto> resultList = em.createQuery(
                "select new com.example.querydsl.dto.MemberDto(m.username, m.age) from Member m",
                MemberDto.class).getResultList();

        System.out.println("resultList = " + resultList);
    }

    @Test
    public void findDataByQuerydslSetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDataByQueryDslField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDataByQueryDslConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDataByQueryDslUserDto(){
        QMember subMember = new QMember("subMember");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(JPAExpressions
                                .select(subMember.age.max())
                                .from(subMember), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    //프로젝션과 결과 반환 @QueryProjection
    //컴파일 시점에 타입체크도 가능하다
    //하지만, Q파일을 생성해야하고 DTO가 QueryDsl에 대한 라이브러리 의존성이 생겨버린다

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto2> result = queryFactory
                .select(new QMemberDto2(member.username, member.age))
                .from(member)
                .fetch();
        for (MemberDto2 memberDto2 : result) {
            System.out.println("memberDto2 = " + memberDto2);
        }
    }

    //동적쿼리 : @BooleanBuilder
    //

    @Test
    public void dynamicQuery_BoolenBuilder(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);

    }

    private List<Member> searchMember1(String usernameParam, Integer ageParam) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameParam != null) {
            builder.and(member.username.eq(usernameParam));
        }
        if (ageParam != null) {
            builder.and(member.age.eq(ageParam));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    //동적쿼리 - where 다중 파라미터 **자주 사용

    @Test
    public void dynamicQuery_whereParam(){
        String usernameParam = "member1";
        Integer ageParam = 10;
        List<Member> result = searchMember2(usernameParam, ageParam);
        List<Member> result2 = searchMember3(usernameParam, ageParam);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
        for (Member member1 : result2) {
            System.out.println("member1 = " + member1);
        }
//        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private List<Member> searchMember3(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        if (usernameCond == null) {
            return null;
        }
        return member.username.eq(usernameCond);
    }

    private BooleanExpression ageEq(Integer ageCond) {
        if (ageCond == null) {
            return null;
        }
        return member.age.eq(ageCond);
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return member.username.eq(usernameCond).and(member.age.eq(ageCond));
    }

    //수정 삭제 벌크 쿼리
    //벌크 연산은 디비에 바로 쿼리가 날라가서 영속성컨텍스트랑 상태가 다르다..
    //
    @Test
    public void bulkUpdate(){
        //영향을 받은 low수가 나옴
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        em.flush();
        em.clear();
        List<Member> fetch = queryFactory.selectFrom(member).fetch();
        for (Member fetch1 : fetch) {
            System.out.println("fetch1 = " + fetch1);
        }
    }

    @Test
    public void bulkAdd(){
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1))
                .execute();
    }

    @Test
    public void bulkDelete(){
        queryFactory
                .delete(member)
                .where(member.age.eq(10))
                .execute();
    }

    @Test
    public void sqlFuntion(){
        List<String> fetch = queryFactory
                .select(
                        Expressions
                                .stringTemplate(
                                        "function('replace', {0},{1},{2})",
                                        member.username,
                                        "member", "M"))
                .from(member)
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunction2(){
        List<String> fetch = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(member.username.lower()))
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username)))
                .fetch();
        for (String s : fetch) {
            System.out.println("s = " + s);
        }
    }

}
