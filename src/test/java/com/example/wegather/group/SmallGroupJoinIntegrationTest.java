package com.example.wegather.group;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.wegather.auth.AuthControllerTest;
import com.example.wegather.group.domain.entity.SmallGroupJoin;
import com.example.wegather.group.domain.repotitory.SmallGroupJoinRepository;
import com.example.wegather.group.dto.CreateSmallGroupRequest;
import com.example.wegather.group.dto.GroupJoinRequestDto;
import com.example.wegather.group.dto.SmallGroupDto;
import com.example.wegather.auth.dto.SignUpRequest;
import com.example.wegather.IntegrationTest;
import com.example.wegather.member.dto.MemberDto;
import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("소모임 가입 통합 테스트")
public class SmallGroupJoinIntegrationTest extends IntegrationTest {
  @Autowired
  private SmallGroupJoinRepository smallGroupJoinRepository;

  private static final String memberPassword = "1234";
  private MemberDto member01;
  private MemberDto member02;
  private MemberDto member03;
  private SmallGroupDto group01;

  @BeforeEach
  void initData() {
    member01 = insertMember("member01","testUser1@gmail.com" ,memberPassword);
    member02 = insertMember("member02","testUser2@gmail.com", memberPassword);
    member03 = insertMember("member03","testUser3@gmail.com", memberPassword);
    group01 = insertSmallGroup("group-01" ,"group01", 100L, member01);
  }

  @Test
  @DisplayName("소모임 가입 요청에 성공합니다.")
  void smallGroupJoinRequest_success() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member02;

    ExtractableResponse<Response> response = requestSmallGroupJoinRequest(smallGroup.getId(), joinMember.getUsername());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
  }

  @Test
  @DisplayName("이미 가입한 회원이어서 소모임 가입 요청에 실패합니다.")
  void smallGroupJoinRequest_fail_because_groupLeader_request() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member01;

    ExtractableResponse<Response> response = requestSmallGroupJoinRequest(
        smallGroup.getId(), joinMember.getUsername());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @DisplayName("최대 회원수를 초과하여 소모임 가입 요청에 실패합니다.")
  void smallGroupJoinRequest_fail_because_exceed_max_member_count() {
    // given
    SmallGroupDto smallGroup = insertSmallGroup("group1","group01", 1L, member01);

    MemberDto joinMember1 = member03;
    // when
    ExtractableResponse<Response> response = requestSmallGroupJoinRequest(smallGroup.getId(), joinMember1.getUsername());
    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @DisplayName("소모임 가입 요청 목록을 조회합니다.")
  void readAllJoinRequests_success() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member02;
    requestSmallGroupJoinRequest(smallGroup.getId(), joinMember.getUsername()); // 가입 요청
    int page = 0;

    ExtractableResponse<Response> response = requestReadAllJoinRequests(
        smallGroup.getId(), page, member01.getUsername());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    List<GroupJoinRequestDto> content = response.jsonPath()
        .getList(".", GroupJoinRequestDto.class);
    GroupJoinRequestDto requestDto = content.get(0);
    assertThat(requestDto.getSmallGroupJoinId()).isEqualTo(group01.getId());
    assertThat(requestDto.getMemberId()).isEqualTo(joinMember.getId());
    assertThat(requestDto.getUsername()).isEqualTo(joinMember.getUsername());
    assertThat(requestDto.getEmail()).isEqualTo(joinMember.getEmail());
    assertThat(requestDto.getProfileImage()).isEqualTo(joinMember.getProfileImage());
  }

  @Test
  @DisplayName("소모임장이 아니라서 소모임 가입 요청 목록 조회에 실패합니다.")
  void readAllJoinRequests_fail_because_not_leader() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member02;
    requestSmallGroupJoinRequest(smallGroup.getId(), joinMember.getUsername()); // 가입 요청
    int page = 0;

    ExtractableResponse<Response> response = requestReadAllJoinRequests(
        smallGroup.getId(), page, member02.getUsername());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  @Test
  @DisplayName("소모임 가입 요청을 승인합니다.")
  void approveSmallGroupJoin_success() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member02;
    requestSmallGroupJoinRequest(smallGroup.getId(), joinMember.getUsername());
    Long requestId = findSmallGroupJoin(smallGroup.getId(), joinMember.getId()).getId();

    ExtractableResponse<Response> response = requestApproveSmallGroupJoin(
        smallGroup.getId(), requestId, member01.getUsername());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
  }

  @Test
  @DisplayName("소모임장이 아니어서 소모임 가입 요청 승인에 실패합니다.")
  void approveSmallGroupJoin_fail_because_not_leader() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member02;
    requestSmallGroupJoinRequest(smallGroup.getId(), joinMember.getUsername());

    SmallGroupJoin smallGroupJoin = findSmallGroupJoin(smallGroup.getId(), joinMember.getId());

    ExtractableResponse<Response> response = requestApproveSmallGroupJoin(
        smallGroup.getId(), smallGroupJoin.getId(), member02.getUsername());

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_FORBIDDEN);
  }

  private SmallGroupJoin findSmallGroupJoin(Long smallGroupId, Long memberId) {
    return smallGroupJoinRepository.findBySmallGroup_IdAndMember_Id(smallGroupId, memberId)
        .orElseThrow(() -> new RuntimeException("소모임 가입 내역을 찾을 수 없습니다."));
  }

  @Test
  @DisplayName("소모임 가입 요청을 거절합니다.")
  void rejectSmallGroupJoin_success() {
    SmallGroupDto smallGroup = group01;
    MemberDto joinMember = member02;
    requestSmallGroupJoinRequest(smallGroup.getId(), joinMember.getUsername());
    Long requestId = findSmallGroupJoin(smallGroup.getId(), joinMember.getId()).getId();
    RequestSpecification spec = AuthControllerTest.signIn(member01.getUsername(), memberPassword);

    ExtractableResponse<Response> response = RestAssured.given().log().ifValidationFails()
        .spec(spec)
        .pathParam("id", smallGroup.getId())
        .pathParam("requestId", requestId)
        .when().post("/api/smallGroups/{id}/join/requests/{requestId}/reject")
        .then().log().ifValidationFails().extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
  }

  public static ExtractableResponse<Response> requestSmallGroupJoinRequest(
      Long smallGroupId, String loginUsername) {
    RequestSpecification spec = AuthControllerTest.signIn(loginUsername, memberPassword);
    ExtractableResponse<Response> response = RestAssured.given().log().ifValidationFails()
        .spec(spec)
        .pathParam("id", smallGroupId)
        .when().post("/api/smallGroups/{id}/join/requests")
        .then().log().ifValidationFails().extract();
    return response;
  }

  public static ExtractableResponse<Response> requestApproveSmallGroupJoin(Long smallGroupId,
      Long requestId, String loginUsername) {
    RequestSpecification spec = AuthControllerTest.signIn(loginUsername, memberPassword);
    ExtractableResponse<Response> response = RestAssured.given().log().ifValidationFails()
        .spec(spec)
        .pathParam("id", smallGroupId)
        .pathParam("requestId", requestId)
        .when().post("/api/smallGroups/{id}/join/requests/{requestId}/approve")
        .then().log().ifValidationFails().extract();
    return response;
  }

  private ExtractableResponse<Response> requestReadAllJoinRequests(Long smallGroupId,
      int page, String loginUsername) {
    RequestSpecification spec = AuthControllerTest.signIn(loginUsername, memberPassword);
    ExtractableResponse<Response> response = RestAssured.given().log().ifValidationFails()
        .spec(spec)
        .pathParam("id", smallGroupId)
        .queryParam("page", page)
        .when().get("/api/smallGroups/{id}/join/requests")
        .then().log().ifValidationFails().extract();
    return response;
  }

  private SmallGroupDto insertSmallGroup(String path, String groupName, Long maxMemberCount, MemberDto loginMember) {
    CreateSmallGroupRequest request = CreateSmallGroupRequest.builder()
        .path(path)
        .name(groupName)
        .shortDescription("테스트입니다.")
        .maxMemberCount(maxMemberCount)
        .build();

    return SmallGroupIntegrationTest.requestCreateGroup(request, loginMember.getUsername())
        .as(SmallGroupDto.class);
  }

  private MemberDto insertMember(String username, String email ,String password) {
    SignUpRequest request = SignUpRequest.builder()
        .username(username)
        .password(password)
        .email(email)
        .build();

    return AuthControllerTest.signUp(request).as(MemberDto.class);
  }
}
