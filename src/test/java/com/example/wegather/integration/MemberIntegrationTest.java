package com.example.wegather.integration;

import static com.example.wegather.global.vo.MemberType.*;
import static io.restassured.module.mockmvc.RestAssuredMockMvc.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.wegather.interest.dto.CreateInterestRequest;
import com.example.wegather.interest.dto.InterestDto;
import com.example.wegather.member.domain.MemberRepository;
import com.example.wegather.global.vo.MemberType;
import com.example.wegather.auth.dto.SignUpRequest;
import com.example.wegather.member.dto.MemberDto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.response.MockMvcResponse;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import java.util.List;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.web.context.WebApplicationContext;

@DisplayName("회원 통합테스트")
class MemberIntegrationTest extends IntegrationTest {

  @Autowired
  MemberRepository memberRepository;

  @Autowired
  private WebApplicationContext webApplicationContext;

  MemberDto member01;
  MemberDto member02;
  MemberDto member03;

  @BeforeEach
  void initRestAssuredApplicationContext() {
    webAppContextSetup(webApplicationContext);
  }

  @BeforeEach
  void init() {
    member01 = insertTestMember("test01", "김지유", ROLE_USER);
    member02 = insertTestMember("test02", "김진주", ROLE_USER);
    member03 = insertTestMember("test03", "박세미", ROLE_USER);
  }

  @Test
  @DisplayName("전체 회원을 조회합니다.")
  @WithMockUser("USER")
  void readAllMembersSuccessfully() {
    // given
    int size = 2;
    int page = 0;

    // when
    ExtractableResponse<MockMvcResponse> response =
        given().log().all()
        .queryParam("size", size, "page", page)
        .contentType(ContentType.JSON)
        .when().get("/members")
        .then().log().all()
        .extract();

    //then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    // 리턴 객체 검증
    List<MemberDto> list = response.jsonPath().getList("content", MemberDto.class);
    assertThat(list).hasSize(2);
    assertThat(list).usingRecursiveComparison().isEqualTo(List.of(member01, member02));

    // 페이징 관련 리턴값 검증
    int pageSize = (int) response.path("pageable.pageSize");
    int pageNumber = (int) response.path("pageable.pageNumber");
    assertThat(pageSize).isEqualTo(size);
    assertThat(pageNumber).isEqualTo(page);
  }

  @Test
  @DisplayName("id로 회원을 조회합니다.")
  @WithMockUser("USER")
  void readOneMemberByIdSuccessfully() {
    // given
    // when
    ExtractableResponse<MockMvcResponse> response =
        given().log().all()
        .contentType(ContentType.JSON)
        .when().get("/members/{id}", member01.getId())
        .then().log().all()
        .extract();

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.body().as(MemberDto.class))
        .usingRecursiveComparison().isEqualTo(member01);
  }

  @Test
  @DisplayName("id로 회원을 삭제합니다.")
  @WithMockUser("USER")
  void deleteMemberByIdSuccessfully() {
    // given
    // when
    ExtractableResponse<MockMvcResponse> response =
        given().log().all()
        .contentType(ContentType.JSON)
        .when().delete("/members/{id}", member01.getId())
        .then().log().all()
        .extract();

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
  }

  @Test
  @DisplayName("회원 프로필 이미지를 수정합니다.")
  @WithMockUser("USER")
  void updateProfileImageSuccessfully() {
//    String controlName = "profileImage";
//    String fileName = "image.jpg";
//    String mediaType = MediaType.TEXT_PLAIN_VALUE;
//    byte[] bytes = "111,222".getBytes();
//
//    // given
//    MultiPartSpecification file = new MultiPartSpecBuilder("111,222".getBytes())
//        .mimeType(MediaType.TEXT_PLAIN_VALUE)
//        .controlName("profileImage")
//        .fileName("image.jpg")
//        .build();
//
//    Long id = member01.getId();
//
//    // when
//    ExtractableResponse<MockMvcResponse> response =
//        given().log().all()
//        .multiPart(controlName, fileName, bytes, mediaType)
//        .when().put("/members/{id}/image", id)
//        .then().log().all()
//        .extract();
//
//    // then
//    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
//    Member findMember = memberRepository.findById(id)
//        .orElseThrow(() -> new RuntimeException("회원이 없습니다."));
//    String storedImage = findMember.getProfileImage().getValue();
//    System.out.println("storedImage : " + storedImage);
//    assertThat(storedImage).isNotEqualTo(DEFAULT_IMAGE_NAME);
//
//    // 이미지 삭제
//
//    given().log().all()
//        .when().delete("/images/{filename}", storedImage)
//        .then().log().all();
  }

  @Test
  @DisplayName("회원의 관심사를 추가합니다.")
  @WithMockUser("USER")
  void addMemberInterestsSuccessfully() {
    // given
    InterestDto interest1 = insertInterest("공부");

    // when
    ExtractableResponse<Response> response = requestAddMemberInterest(interest1);

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    List<InterestDto> interestList = response.jsonPath().getList(".", InterestDto.class);
    assertThat(interestList).extracting("name")
        .contains(interest1.getName());
  }

  private ExtractableResponse<Response> requestAddMemberInterest(InterestDto interest1) {
    ExtractableResponse<Response> response =
        RestAssured.given().log().all()
            .auth().basic("test01", "password")
            .queryParam("interestId", interest1.getId())
            .contentType(ContentType.JSON)
            .when().post("/members/{id}/interests", member01.getId())
            .then().log().all()
            .extract();
    return response;
  }

  @Test
  @DisplayName("회원의 관심사를 삭제합니다.")
  @WithMockUser("USER")
  void removeMemberInterestsSuccessfully() {
    // given
    InterestDto interest1 = insertInterest("공부");
    requestAddMemberInterest(interest1);

    // when
    ExtractableResponse<Response> response =
        RestAssured.given().log().all()
            .auth().basic("test01", "password")
            .queryParam("interestId", interest1.getId())
            .contentType(ContentType.JSON)
            .when().delete("/members/{id}/interests", member01.getId())
            .then().log().all()
            .extract();

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    List<InterestDto> interestList = response.jsonPath().getList(".", InterestDto.class);
    assertThat(interestList).extracting("name")
        .doesNotContain(interest1.getName());
  }

  private InterestDto insertInterest(String interestName) {
    CreateInterestRequest request = CreateInterestRequest.builder()
        .interestName(interestName)
        .build();

    return given()
        .body(request)
        .contentType(ContentType.JSON)
        .when().post("/interests")
        .then()
        .extract().as(InterestDto.class);
  }


  private MemberDto insertTestMember(String username, String name, MemberType memberType) {
    SignUpRequest request = SignUpRequest.builder()
        .username(username)
        .password("password")
        .name(name)
        .phoneNumber("010-1234-1234")
        .memberType(memberType)
        .build();

    return insertMember(request).as(MemberDto.class);
  }

  private ExtractableResponse<MockMvcResponse> insertMember(SignUpRequest request) {
     return given().log().all()
        .body(request)
        .contentType(ContentType.JSON)
        .when().post("/sign-up")
        .then().log().all()
        .extract();
  }
}
