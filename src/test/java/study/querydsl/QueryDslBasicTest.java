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
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static com.querydsl.jpa.JPAExpressions.select;
import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QueryDslBasicTest {

    @PersistenceContext
    EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before(){
        //필드 레벨로 가지고 간다.
        queryFactory = new JPAQueryFactory(em);

        //시작 전에 실행시켜준다.
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10,teamA);
        Member member2 = new Member("member2", 20,teamA);

        Member member3 = new Member("member3", 30,teamB);
        Member member4 = new Member("member4", 40,teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL(){
        //member1을 찾아라
        Member findMember = em.createQuery("select m from Member m " +
                "where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }



    @Test
    public void startQueryDsl(){
        //JPAQueryFactory queryFactory = new JPAQueryFactory(em);
        //QMember m = new QMember("m");//이름을 주어야된다.
        //QMember m = QMember.member; //내부에 만들어진다.

        //Q 타입을 만들어 컴파일 시점에 오류를 잡을수 있다.
        Member findMember = queryFactory
                .select(member) //static import로 더 깔끔하게 사용 가능하다.
                .from(member)//jpql에 alias가 member1로 되게 되는데 내부에 member1로 선언했기 때문이다.
                .where(member.username.eq("member1"))//prepareStatement 에 파라미터 바인딩 방식을 사용한다.
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search(){
        Member findMember = queryFactory
                .selectFrom(member) //selectFrom 으로 합칠수있다.
                .where(member.username.eq("member1")
                        .and(member.age.eq(10))) //메소드 체인의 and나 or로 걸수 있다.
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }
    @Test
    public void searchAndParam(){
        Member findMember = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1")
                        ,(member.age.between(10,30)) //and와 같은경우 쉼표로 줄수 있다.
                )
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void resultFetch(){
        /*
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchFirst = queryFactory
                .selectFrom(member)
                .fetchFirst();
        */
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .fetchResults();
        results.getTotal(); //count 쿼리가 실행된다.
        List<Member> content = results.getResults();

        long total = queryFactory
                .selectFrom(member)
                .fetchCount(); //count 쿼리만 실행된다.
    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단, 2에서 회원 이름이 없으면 마지막에 출력(nulls last)
     */
    @Test
    public void sort(){
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
    @Test
    public void paging1(){
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();
        QueryResults<Member> queryResults = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(queryResults.getTotal()).isEqualTo(4);
        assertThat(queryResults.getLimit()).isEqualTo(2);
        assertThat(queryResults.getOffset()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(2);
    }
    @Test
    public void aggregation(){
        Tuple result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetchOne();

        assertThat(result.get(member.count())).isEqualTo(4);
        assertThat(result.get(member.age.sum())).isEqualTo(100);
        assertThat(result.get(member.age.avg())).isEqualTo(25);
        assertThat(result.get(member.age.max())).isEqualTo(40);
        assertThat(result.get(member.age.min())).isEqualTo(10);

    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라
     */
    @Test
    public void group() throws Exception{
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();
        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * 팀 A에 소속된 모든 회원
     * @throws Exception
     */
    @Test
    public void join() throws Exception{
        List<Member> result = queryFactory
                .selectFrom(member)
                //.innerJoin(member.team, team)
                .leftJoin(member.team, team) //leftJoin 도 가능하다
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1","member2");
    }

    /**
     * 세타조인
     * 회원의 이름이 팀이름과 같은 회원 조인
     * @throws Exception
     */
    @Test
    public void thetaJoin() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team) //from 절에 나열한다. //외부조인이 불가능하다 -> on을 사용하면 외부조인 가능
                .where(member.username.eq(team.name))
                .fetch();
        assertThat(result)
                .extracting("username")
                .containsExactly("teamA","teamB");
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL : select m,t from Member m left join m.team t on t.name = 'teamA'
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception{
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
                //.on(team.name.eq("teamA"))
                .where(team.name.eq("teamA")) //innerJoin일 경우 where에 있나 on을 사용하나 똑같다
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 연관관계 없는 엔티티 외부 조인
     * 회원의 이름이 팀이름과 같은 대상 외부 조인
     * @throws Exception
     */
    @Test
    public void join_on_no_relation() throws Exception{
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team) //연관관계가 있을경우 leftJoin(member.team, team) 이렇게 하였지만 막조인 같은경우 team을 그냥 넣는다
                .on(member.username.eq(team.name))
                .fetch();
        for(Tuple tuple : result){
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() throws Exception{
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoinUse() throws Exception{
        em.flush();
        em.clear();
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .join(member.team,team)
                .fetchJoin() //페치조인이 들어가면 된다
                .where(QMember.member.username.eq("member1"))
                .fetchOne();

        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치 조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원 조회
     */
    @Test
    public void subQuery(){
        //Alias가 중복되면 안될경우 이렇게 만들어 주어야한다.
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(30,40);
    }



    @Test
    public void subQueryIn(){
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))
                ))
                .fetch();
        assertThat(result).extracting("age")
                .containsExactly(20,30,40);
    }

    @Test
    public void selectSubQuery(){
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                    select(memberSub.age.avg())
                    .from(memberSub)
                )
                .from(member)
                .fetch();
        for(Tuple tuple : result)
            System.out.println("tuple = " + tuple);
    }

    @Test
    public void basicCase(){
        List<String> result = queryFactory
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스물살")
                                .otherwise("기타")
                ).from(member)
                .fetch();
        for(String s : result)
            System.out.println(s);
    }

    @Test
    public void complexCase(){
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 30)).then("21~30살")
                        .otherwise("기타")
                ).from(member)
                .fetch();
        for(String s : result)
            System.out.println(s);
    }

    @Test
    public void constant(){
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();
        for(Tuple tuple : result)
            System.out.println(tuple);
    }

    @Test
    public void concat(){
        //username_age
        String result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();
        System.out.println("s = " + result);
    }

    @Test
    public void simpleProjection(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();
        for(String s : result){
            System.out.println("s = " + s);
        }
    }

    @Test
    public void tupleProjection(){
        //Tuple 은 Repository 까지 사용하는것은 괜찬지만, 컨트롤러,서비스 계층에서 사용하면 안된다.
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();
        for(Tuple tuple : result){
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " +username);
            System.out.println("age = "+ age);
        }
    }

    @Test
    public void findDtoByJPQL(){
        //패키지명을 다 적는 불편함이 있다.
        List<MemberDto> result = em.createQuery(
                "select new study.querydsl.dto.MemberDto(m.username,m.age) "
                + "from Member m", MemberDto.class)
                .getResultList();
        for(MemberDto memberDto: result){
            System.out.println("memberDto = " + memberDto);
        }
    }
    @Test
    public void findDtoBySetter(){
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, //Getter,Setter를 사용
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto: result){
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByField(){
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, //field에 바로 주입한다
                        member.username,
                        member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto: result){
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findDtoByConstructor(){
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, //생성자로 주입해 준다
                        member.username, //타입이 딱 맞아야된다.
                        member.age))
                        //member.id)) //런타임에 에러가 발생한다.
                .from(member)
                .fetch();
        for(MemberDto memberDto: result){
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void findUserDtoByField(){
        QMember memberSub = new QMember("memberSub");
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),  //이름이 안맞기때문에 null이 들어간다..
                        //ExpressionUtils.as(member.username,"name"), //이렇게 사용해도 된다.
                        ExpressionUtils.as(
                                JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub),"age") //2번째 파라미터로 alias를 줄수 있다.
                        ))
                .from(member)
                .fetch();
        for(UserDto userDto: result){
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void findDtoByQueryProjection(){
        List<MemberDto> result = queryFactory
                //생성자를 통한 생성방식은 런타임시 에러가 발생되지만, 해당 방식은 컴파일 오류가 발생(좋은 방식)
                //단점 1. Q파일을 생성해야된다 2. DTO는 QueryDsl에 의존성이 없었다가 Q파일을 생성하게 된 순간, 의존성을 가지게 된다.
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();
        for(MemberDto memberDto : result)
            System.out.println("MemberDto = " + memberDto);
    }

    @Test
    public void dynamicQuery_BooleanBuilder(){
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMember1(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();//생성자로 초기값을 넣어줄 수도 있다.
        if(usernameCond != null)
            builder.and(member.username.eq(usernameCond)); //builder 에 더해준다.
        if(ageCond != null)
            builder.and(member.age.eq(ageCond));
        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    @Test
    public void dynamicQuery_whereParam(){
        String usernameParam = "member1";
        Integer ageParam = null;
        List<Member> result = searchMember2(usernameParam,ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory //바로 쿼리를 볼수 있는 장점이 있다.
                .selectFrom(member)
                //.where(usernameEq(usernameCond),ageEq(ageCond)) //where에 null이 들어가면 무시한다.
                .where(allEq(usernameCond,ageCond)) //조합이 가능하다.
                .fetch();
    }

    private BooleanExpression usernameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }
    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }


    private BooleanExpression allEq(String usernameCond, Integer ageCond){
        //조립을 할 수 있다(자바 코드이기 때문에 합쳐서 해결이 가능한 것이다.)
        //다른 코드에서 재활용이 가능하다.
        return usernameEq(usernameCond).and(ageEq(ageCond));
    }

    @Test
    @Commit
    public void bulkUpdate(){
        //member1 = 10 -> DB member1
        //member2 = 20 -> DB member2
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4

        //해당 값들이 다 영속성 컨택스에 올라가 있는 상태이다.
        //벌크 연산은 DB에 바로 쿼리를 동작시키기 때문에 DB와 영속성 컨택스트랑 데이터가 맞지 않는다.
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();
        //영속성 컨택스트에 값이 있기 때문에 DB에서 값을 가지고 와도 버리기 때문에 결과적으로 반영되지 않은 데이터를 가지고 잇다.
        //member1 = 10 -> DB 비회원
        //member2 = 20 -> DB 비회원
        //member3 = 30 -> DB member3
        //member4 = 40 -> DB member4

        em.flush();
        em.clear(); //영속성 컨택스트를 초기화 해버리면 된다.

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();
        for(Member member : result)
            System.out.println(member);

    }

    @Test
    public void bulkAdd(){
        queryFactory
                .update(member)
                //.set(member.age, member.age.add(1)) //뺴고 싶다면 -1을 넣어주면 된다
                .set(member.age, member.age.multiply(2)) //곱하기
                .execute();
    }


    @Test
    public void bulkDelete(){
        //삭제
        long count = queryFactory
                .delete(member)
                .where(member.age.gt(10))
                .execute();
    }

    @Test
    public void sqlFunction(){
        List<String> result = queryFactory
                .select(Expressions.stringTemplate(
                        "function('replace', {0}, {1}, {2})",
                        member.username, "member", "M"))
                .from(member)
                .fetch();
        for(String s : result)
            System.out.println(s);
    }

    @Test
    public void sqlFunction2(){
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .where(member.username.eq(
                       // Expressions.stringTemplate("function('lower',{0})" , member.username)
                        member.username.lower() //기본적으로 DB에서 제공되는 안씨표준은 거의 다 지원한다.
                ))
                .fetch();
        for(String s : result)
            System.out.println(s);
    }

}
