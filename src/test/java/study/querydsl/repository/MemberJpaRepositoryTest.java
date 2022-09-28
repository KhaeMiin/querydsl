package study.querydsl.repository;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    @Test
    void basicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        //containsExactly: 순서를 포함해서 정확히 일치
        //containsOnly: 순서, 중복을 무시하는 대신 값이 정확히 일치
        List<Member> result1 = memberJpaRepository.findAll_Querydsl();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    void searchTest() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
//        condition.setAgeGoe(20); //35 같거나 이상인 것들
//        condition.setAgeLoe(40); //40 같거나 이하인 것들
//        condition.setTeamName("teamB"); // teamB인 것들
        //조건이 없을 경우 데이터를 다 끌고옴 (그래서 기본 조건이 있는 것이 좋음, 또는 페이징 쿼리)

        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(condition);

        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }
    @Test
    void searchTest2() {
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

        MemberSearchCondition condition = new MemberSearchCondition();
        condition.setAgeGoe(20); //35 같거나 이상인 것들
        condition.setAgeLoe(40); //40 같거나 이하인 것들
        condition.setTeamName("teamB"); // teamB인 것들
        //조건이 없을 경우 데이터를 다 끌고옴 (그래서 기본 조건이 있는 것이 좋음, 또는 페이징 쿼리)

        List<MemberTeamDto> result = memberJpaRepository.search(condition);

        assertThat(result).extracting("username").containsExactly("member3", "member4");
    }

}