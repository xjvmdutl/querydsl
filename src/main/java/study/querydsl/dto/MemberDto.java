package study.querydsl.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor //기본 생성자를 만들어 주어야 한다.
public class MemberDto {
    private String username;
    private int age;

    @QueryProjection //DTO도 Q파일로 생성된다.
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
}
