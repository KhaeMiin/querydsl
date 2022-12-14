package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

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
    void startJPQL() {
        //member1??? ?????????
        String qlString =
                "select m from Member m " +
                "where m.username = :username"; //????????? RUNTIME EX ?????? (????????? ?????? ??????)
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
//        QMember m = new QMember("m"); //"m" : ??????????????? (?????? ???????????? ??????) > ????????? ?????????.
//        QMember m = QMember.member;

        Member findMember = queryFactory
                .select(member) //QMember.member?????? QMember static import > ??? ????????? ??????
                .from(member)
                .where(member.username.eq("member1")) //????????? ?????????????????? ?????? (????????? ?????? ?????????)
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //?????? ?????? ??????
    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void searchAndParam() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        (member.age.eq(10))) //?????? ','??? and ???????????? ????????? ????????????.
                .fetchOne(); //?????? ?????? ??????(?????? ????????? null, ??? ???????????? ????????????)

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void resultFetch() {
//        List<Member> fetch = queryFactory
//                .selectFrom(member)
//                .fetch();
//
//        Member fetchOne = queryFactory
//                .selectFrom(member)
//                .fetchOne();
//
//        Member fetchFirst = queryFactory
//                .selectFrom(member)
//                .fetchFirst();

//        QueryResults<Member> results = queryFactory
//                .selectFrom(member)
//                .fetchResults();
//        results.getTotal();
//        List<Member> count = results.getResults();

        queryFactory
                .selectFrom(member)
                .fetchCount();
    }

    /**
     * Querydsl ?????? ?????? (jpql??? ???????????? ?????? ?????? ?????? ??????)
     */
    @Test
    void resultTest() {
        Member findUsername = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member2"))
                .fetchOne();
        List<Member> findUsernames = queryFactory
                .selectFrom(member)
                .where(member.username.ne("member1")) //username != 'member1'
                .fetch();
        List<Member> findNotEmpty = queryFactory
                .selectFrom(member)
                .where(member.username.isNotEmpty()) //empty, NotNull ??? ?????? ??????
                .fetch();
        List<Member> findGoe = queryFactory
                .selectFrom(member)
                .where(member.age.goe(30)) // age >= 30
                .fetch();
        List<Member> findGt = queryFactory
                .selectFrom(member)
                .where(member.age.gt(30)) // age > 30
                .fetch();
        List<Member> findLoe = queryFactory
                .selectFrom(member)
                .where(member.age.loe(30)) // age <= 30
                .fetch();
        List<Member> findLt = queryFactory
                .selectFrom(member)
                .where(member.age.lt(30)) //age < 30
                .fetch();
    }

    /**
     * ?????? ?????? ??????
     * 1. ?????? ?????? ???????????? (desc)
     * 2. ?????? ?????? ???????????? (asc)
     * ??? 2.?????? ?????? ????????? ????????? ???????????? ??????(nulls last)
     */
    @Test
    void sort() {
        //given
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        //when
        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);
        //then
        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    /**
     * ?????????
     */
    @Test
    void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //1??? ???????????? ?????? (?????? 0?????? ??????)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //1??? ???????????? ?????? (?????? 0?????? ??????)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * ??????
     * Tuple (??????????????? ?????? ?????? ?????????.): ?????????????????? ???????????? ????????? ??? ????????????.
     */
    @Test
    void aggregation() {
        List<Tuple> result = queryFactory //Tuple: querydsl?????? ??????
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40); //????????? ??? ??????
        assertThat(tuple.get(member.age.min())).isEqualTo(10); //????????? ??? ??????

    }

    /**
     * ?????? ????????? ??? ?????? ?????? ????????? ?????????
     */
    @Test
    void group() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //?????? ??????

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); //?????? ??????

    }

    /**
     * join
     */
    @Test
    void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }

    /**
     * ???????????? ?????? ????????????
     * ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ??????(????????? ?????? ?????? ??????)
     */
    @Test
    void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * ?????? on???
     * ???) ????????? ?????? ???????????????, ??? ????????? teamA??? ?????? ??????, ????????? ?????? ??????
     * JPQL: select m, t from Member m lsft join m.team t on t.name = 'teamA'
     * outer join: ????????? ?????? ?????? ?????? null??? ????????? ????????? (????????? ???????????? ????????? ??? ?????????)
     * inner: ?????? ?????? ???????????? ???????????? ?????????.
     */
    @Test
    void join_in_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team).on(team.name.eq("teamA")) //outer join?????? on?????? ????????? ??? ?????? ??????.
                .join(member.team, team).where(team.name.eq("teamA")) //inner join ?????? where?????? on????????? ????????? ?????? (????????? ?????? where??????)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }



    /**
     * ???????????? ?????? ????????? ?????? ??????
     * ????????? ????????? ??? ????????? ?????? ?????? ?????? ??????
     */
    @Test
    void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team).on(member.username.eq(team.name)) //?????? ????????? ??? ????????? ?????? ?????? join, ?????? ????????? null (null ???????????? ???????????? inner join ??????)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;
    /**
     * ?????? ??????
     */
    @Test
    void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1")) //team??? ?????? ????????? ???
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// ?????? ???????????? ??? entity?????? ?????? entity?????? ??? ??? ??????.
        assertThat(loaded).as("?????? ?????? ?????????").isFalse();
    }
    @Test
    void fetchJoinUse() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());// ?????? ???????????? ??? entity?????? ?????? entity?????? ??? ??? ??????.
        assertThat(loaded).as("?????? ?????? ?????????").isTrue();
    }

    /**
     * ?????? ??????
     * ????????? ?????? ?????? ?????? ??????
     */
    @Test
    void subQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }
    /**
     * ?????? ??????2
     * ????????? ?????? ?????? ?????? ??????
     */
    @Test
    void subQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * ?????? ??????2
     * ????????? 10??? ?????? ?????? ??????
     */
    @Test
    void subQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) //10??? ??????
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    @Test
    void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions //static import ?????? (????????? ??? ?????? ???????????? ???????????????~)
                                .select(member.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * Case???
     */
    @Test
    void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("??????")
                        .when(20).then("?????????")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20???")
                        .when(member.age.between(21, 30)).then("21~30???")
                        .otherwise("??????"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * ?????? (??? ?????? ?????????
     */
    @Test
    void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * ?????? ????????? concat()??????
     */
    @Test
    void concat() {

        //{username}_{age} ????????? ???????????? ??????
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue())) //age??? ????????? String?????? ??????????????? stringValue()??????
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s); //s = member1_10
        }
    }

    /**
     * ????????????? select?????? ????????? ????????? ????????? ????????? ???????????? ???
     * ????????? ????????? ????????? ???????????? ??????
     * ????????? ??? ???????????? ???????????? DTO??? ??????
     */
    @Test
    void simpleProjection() {
        List<String> result = queryFactory
                .select(member.username) //????????? ??????
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void tupleProjection() {
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

    /**
     * DTO??? ????????????
     * ??????: JPQL, setter, fields, constructor
     */
    @Test
    void findDtoByJPQL() {
        List<MemberDto> result = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age)" +
                        " from Member m", MemberDto.class)
                .getResultList();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findBySetter() {
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
    public void findByField() {
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //getter, setter ???????????? (?????? ???????????? ???????????????)
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor() {
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, //getter, setter ???????????? (???????????? ???????????? ????????? ????????????)
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDto() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class, //getter, setter ???????????? (???????????? ???????????????)
                        member.username.as("name"), //???????????? ?????? ?????? as()??? ???????????? ???????????? ??????????????????.

                        ExpressionUtils.as(JPAExpressions //subQuery ??????
                                .select(memberSub.age.max()) //??????????????? ??????
                                .from(memberSub), "age") //alias??? ????????? ???????????? ???????????? ????????? ??? ????????? ?????? (?????? ??? ??? ??????)
                ))
                .from(member)
                .fetch();
        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /**
     * DTO > @QueryProjection
     */
    @Test
    void findDtoByQueryProjection() {
        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * BooleanBuilder??? ???????????? ????????????
     * ????????? ????????? ?????????. (???????????? ??????)
     */
    @Test
    void dynamicQuery_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = null; //age??? null?????? username??? ???????????? ??????.
//        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {

        BooleanBuilder builder = new BooleanBuilder();
        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }

        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /**
     * Where ?????? ???????????? ???????????? ???????????? ?????????
     * ????????? ?????? ??????. (???????????? ??????)
     */
    @Test
    void dynamicQuery_WhereParam() {
        String usernameParam = "member1";
        Integer ageParam = null;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(usernameEq(usernameCond), ageEq(ageCond)) //where??? null??? ???????????? ????????? ??????., ???????????? ?????? ???????????? ???????????? ??? ??? ??????.
//                .where(allEq(usernameCond, ageCond))
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null; //null?????? ??????
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null; //null?????? ??????
    }


    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    /**
     * ??????, ?????? ?????? ??????
     */
    @Test
    void bulkUpdate() {

        //member1 = 10 > ?????????
        //member2 = 20 > ?????????
        //member3 = 30 > ??????
        //member4 = 40 > ??????

        long count = queryFactory
                .update(member)
                .set(member.username, "?????????")
                .where(member.age.lt(28))
                .execute();

        //??????: ?????? ?????? ???????????? ????????? ???????????? ?????? ?????? ????????? ?????????????????? ????????? ?????? ???????????? ??????

        //????????? ?????? ????????? ???????????? ????????? ??????????????? ???????????? ????????? ???????????? ?????? ???????????? ?????? ??????????????????????????? ?????? ????????????.
        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
//            System.out.println("member1 = " + member1); //???????????? ????????? ???????????? ?????? ????????? ???????????? ?????? ???????????? ?????? ??? ??? ??????.

        }

        //???????????? ????????? ?????? ??? ??????????????? ?????????~
        em.flush();
        em.clear();

        List<Member> result2 = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member2 : result2) {
            System.out.println("member2 = " + member2); //???????????? ????????? ???????????? ?????? ????????? ???????????? ?????? ???????????? ?????? ??? ??? ??????.

        }
    }

    @Test
    void bulkAdd() {
        long count = queryFactory
                .update(member)
//                .set(member.age, member.age.add(1))
                .set(member.age, member.age.multiply(2))
                .execute();
    }

    @Test
    @DisplayName("delete")
    void bulkDelete() {
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(18)) //18??????
                .execute();
    }

    /**
     * SQL function ??????
     *  > sql????????? ????????? ?????? ????????? ?????? ??? ??????..! (????????? ??????)
     *  https://wakestand.tistory.com/503 function ????????? ?????? ?????????
     *
     *  SQL function ?????? > ???????????? ?????? ????????? ????????? (??? ??????)
     */
    @Test
    void sqlFunction() {
        List<String> result = queryFactory
                .select(Expressions.stringTemplate("function('replace', {0}, {1}, {2})",
                        member.username, "member", "M")) //username?????? 'member'?????? ????????? 'M'?????? ?????????
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    void sqlFunction2() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(
//                        Expressions.stringTemplate("function('lower', {0})", member.username))) // ???????????? ?????? ??????~
                .where(member.username.eq(member.username.lower())) //??? ????????? ??? ????????? ??????!!!?????????!!
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}