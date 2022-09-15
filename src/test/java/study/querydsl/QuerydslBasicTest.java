package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.*;

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
        //member1을 찾아라
        String qlString =
                "select m from Member m " +
                "where m.username = :username"; //오류시 RUNTIME EX 발생 (런타임 에러 극혐)
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    void startQuerydsl() {
//        QMember m = new QMember("m"); //"m" : 별칭같은거 (크게 중요하지 않음) > 나중에 안쓴다.
//        QMember m = QMember.member;

        Member findMember = queryFactory
                .select(member) //QMember.member에서 QMember static import > 이 사용법 권장
                .from(member)
                .where(member.username.eq("member1")) //오류시 컴파일시점에 발생 (컴파일 에러 좋아용)
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    //검색 조건 쿼리
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
                        (member.age.eq(10))) //쉼표 ','로 and 대신으로 사용이 가능하다.
                .fetchOne(); //결과 단건 조회(결과 없으면 null, 둘 이상이면 에러발생)

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
     * Querydsl 검색 조건 (jpql이 제공하는 모든 검색 조건 제공)
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
                .where(member.username.isNotEmpty()) //empty, NotNull 등 사용 가능
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

}
