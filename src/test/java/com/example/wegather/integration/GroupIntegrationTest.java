package com.example.wegather.integration;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.wegather.global.vo.MemberType;
import com.example.wegather.group.domain.Group;
import com.example.wegather.group.domain.repotitory.GroupRepository;
import com.example.wegather.group.dto.CreateGroupRequest;
import com.example.wegather.group.dto.GroupDto;
import com.example.wegather.group.dto.GroupSearchCondition;
import com.example.wegather.group.dto.UpdateGroupRequest;
import com.example.wegather.member.dto.JoinMemberRequest;
import com.example.wegather.member.dto.MemberDto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import org.apache.http.HttpStatus;
import org.hibernate.AssertionFailure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

@DisplayName("소모임 통합 테스트")
public class GroupIntegrationTest extends IntegrationTest {

  @Autowired
  private WebApplicationContext webApplicationContext;

  @Autowired
  private GroupRepository groupRepository;

  @BeforeEach
  void initRestAssuredApplicationContext() {
    RestAssuredMockMvc.webAppContextSetup(webApplicationContext);
  }

  private static final String memberUsername = "test01";
  private static final String memberPassword = "1234";
  private MemberDto member01;
  private GroupDto group01;
  private GroupDto group02;
  private GroupDto group03;

  @BeforeEach
  void initData() {
    member01 = insertMember(memberUsername, memberPassword, MemberType.ROLE_USER);
    group01 = insertGroup("탁사모", 300);
    group02 = insertGroup("책사모", 100);
    group03 = insertGroup("토사모", 200);
  }

  @Test
  @DisplayName("소모임을 생성합니다.")
  void createInterestSuccessfully() {

    CreateGroupRequest request = CreateGroupRequest.builder()
        .groupName("볼사모")
        .description("볼링을 사랑하는 사람들의 모임입니다.")
        .streetAddress("서울특별시 중구 세종대로 125")
        .maxMemberCount(300)
        .build();

    ExtractableResponse<Response> response = RestAssured.given().log().all()
        .auth().basic(memberUsername, memberPassword)
        .body(request)
        .contentType(ContentType.JSON)
        .when().post("/groups")
        .then().log().all()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_CREATED);
    GroupDto result = response.body().as(GroupDto.class);
    assertThat(result)
        .usingRecursiveComparison()
        .ignoringFields("id", "leaderId", "leaderUsername")
        .isEqualTo(request);
  }

  @Test
  @DisplayName("id로 소그룹 조회를 성공합니다.")
  void getGroupSuccessfully() {
    Long groupId = group01.getId();

    ExtractableResponse<Response> response = RestAssured.given().log().all()
        .auth().basic(memberUsername, memberPassword)
        .pathParam("id", groupId)
        .contentType(ContentType.JSON)
        .when().get("/groups/{id}")
        .then().log().all()
        .extract();

    GroupDto result = response.body().as(GroupDto.class);
    assertThat(result)
        .usingRecursiveComparison()
        .isEqualTo(group01);
  }

  @Test
  @DisplayName("그룹 이름과 최대회원수 범위로 소그룹 조회를 성공합니다.")
  void searchGroupByGroupNameAndMaxMemberCountRangeSuccessfully() {
    GroupSearchCondition groupSearchCondition = GroupSearchCondition.builder()
        .groupName("사모")
        .maxMemberCountFrom(100)
        .maxMemberCountTo(201)
        .build();
    int size = 2;
    int page = 0;

    ExtractableResponse<Response> response = RestAssured.given().log().all()
        .auth().basic(memberUsername, memberPassword)
        .body(groupSearchCondition)
        .queryParam("size", size, "page", page)
        .contentType(ContentType.JSON)
        .when().get("/groups")
        .then().log().all()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    List<GroupDto> result = response.jsonPath().getList("content", GroupDto.class);
    assertThat(result).hasSize(2);
    assertThat(result).usingRecursiveFieldByFieldElementComparator().contains(group02, group03);
    // 페이징 관련 리턴값 검증
    int pageSize = (int) response.path("pageable.pageSize");
    int pageNumber = (int) response.path("pageable.pageNumber");
    assertThat(pageSize).isEqualTo(size);
    assertThat(pageNumber).isEqualTo(page);
  }

  @Test
  @DisplayName("소모임 정보를 수정합니다.")
  void updateGroupSuccessfully() {
    UpdateGroupRequest request = UpdateGroupRequest.builder()
        .groupName("수정 모임")
        .description("수정하였습니다.")
        .streetAddress("수정광역시 수정로")
        .maxMemberCount(123)
        .build();

    ExtractableResponse<Response> response = RestAssured
        .given().log().all()
        .auth().basic(memberUsername, memberPassword)
        .pathParam("id", group01.getId())
        .body(request)
        .contentType(ContentType.JSON)
        .when().put("/groups/{id}")
        .then().log().ifValidationFails()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    Group updated = groupRepository.findById(group01.getId())
        .orElseThrow(() -> new AssertionFailure("group ID를 찾을 수 없습니다."));
    assertThat(updated.getName()).isEqualTo(request.getGroupName());
    assertThat(updated.getDescription()).isEqualTo(request.getDescription());
    assertThat(updated.getAddress().getStreetAddress()).isEqualTo(request.getStreetAddress());
    assertThat(updated.getMaxMemberCount().getValue()).isEqualTo(request.getMaxMemberCount());
  }

  private GroupDto insertGroup(String groupName, Integer maxMemberCount) {
    CreateGroupRequest request = CreateGroupRequest.builder()
        .groupName(groupName)
        .description("테스트입니다.")
        .streetAddress("서울특별시 중구 세종대로 125")
        .maxMemberCount(maxMemberCount)
        .build();

    return RestAssured.given().log().all()
        .auth().basic(memberUsername, memberPassword)
        .body(request)
        .contentType(ContentType.JSON)
        .when().post("/groups")
        .then().log().ifStatusCodeIsEqualTo(HttpStatus.SC_CREATED)
        .extract().as(GroupDto.class);
  }

  private MemberDto insertMember(String username, String password, MemberType memberType) {
    JoinMemberRequest request = JoinMemberRequest.builder()
        .username(username)
        .password(password)
        .name("testUser")
        .phoneNumber("010-1234-1234")
        .streetAddress("서울시 강남구 백양대로 123-12")
        .longitude(123.12312)
        .latitude(23.131)
        .memberType(memberType)
        .build();

    return given().body(request).contentType(ContentType.JSON)
        .when().post("/members")
        .then().extract().as(MemberDto.class);
  }
}
