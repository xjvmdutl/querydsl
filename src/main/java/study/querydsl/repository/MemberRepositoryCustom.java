package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

public interface MemberRepositoryCustom {
    List<MemberTeamDto> search(MemberSearchCondition condition);
    //카운트쿼리와 페이지 쿼리 같이나감
    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);
    //카운트쿼리와 페이지 쿼리가 따로 나감
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);
    
}
