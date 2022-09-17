package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.QTeam;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

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

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순 (desc)
     * 2. 회원 이름 올림차순 (asc)
     * 단 2.에서 회원 이름이 없으면 마지막에 출력(nulls last)
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
     * 페이징
     */
    @Test
    void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //1개 스팁하고 시작 (원래 0부터 시작)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    void paging2() {
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1) //1개 스팁하고 시작 (원래 0부터 시작)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(queryResults.getResults().size()).isEqualTo(2);
    }

    /**
     * 집합
     * Tuple (실무에서는 많이 쓰지 않는다.): 데이터타입이 여러개가 들어올 때 사용한다.
     */
    @Test
    void aggregation() {
        List<Tuple> result = queryFactory //Tuple: querydsl에서 제공
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
        assertThat(tuple.get(member.age.max())).isEqualTo(40); //나이가 젤 많은
        assertThat(tuple.get(member.age.min())).isEqualTo(10); //나이가 젤 적은

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
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
        assertThat(teamA.get(member.age.avg())).isEqualTo(15); //평균 연령

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35); //평균 연령

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
     * 연관관계 없이 조인하기
     * 세타 조인
     * 회원의 이름이 팀 이름과 같은 회원 조회(예제를 위한 억지 예제)
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
}
