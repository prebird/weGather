package com.example.wegather.interest;

import static org.assertj.core.api.Assertions.*;

import com.example.wegather.IntegrationTest;
import com.example.wegather.auth.dto.SignInRequest;
import com.example.wegather.auth.dto.SignUpRequest;
import com.example.wegather.interest.dto.CreateInterestRequest;
import com.example.wegather.interest.dto.InterestDto;
import com.example.wegather.member.dto.MemberDto;
import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("관심사 통합테스트")
public class InterestIntegrationTest extends IntegrationTest {

  private static final String PASSWORD = "1234";
  MemberDto member01;
  @BeforeEach
  void init() {
     member01 = insertMember("member01", "testUser1@gmail.com" ,PASSWORD);
  }

  @Test
  @DisplayName("관심사를 생성합니다.")
  void createInterestSuccessfully() {
    RequestSpecification spec = sigIn(member01.getUsername(), PASSWORD);

    CreateInterestRequest request = CreateInterestRequest.builder()
        .interestName("축구")
        .build();

    ExtractableResponse<Response> response =
        RestAssured.given().log().ifValidationFails().spec(spec)
        .body(request).contentType(ContentType.JSON)
        .when().post("/api/interests")
        .then().log().ifValidationFails()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_CREATED);
    assertThat(response.body().as(InterestDto.class).getName())
        .isEqualTo(request.getInterestName());
  }

  @Test
  @DisplayName("관심사 명이 이미 존재하는 경우 예외를 던집니다.")
  void createInterestFailWhenNameAlreadyExists() {
    // given
    RequestSpecification spec = sigIn(member01.getUsername(), PASSWORD);
    String interestName = "아구";
    insertInterest(interestName, member01.getUsername());

    CreateInterestRequest request = CreateInterestRequest.builder()
        .interestName(interestName)
        .build();

    // when
    ExtractableResponse<Response> response = RestAssured.given().log().ifValidationFails().spec(spec)
        .body(request).contentType(ContentType.JSON)
        .when().post("/api/interests")
        .then().log().ifValidationFails()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  @DisplayName("전체 관심사를 조회합니다.")
  void readAllInterestSuccessfully() {
    // given
    RequestSpecification spec = sigIn(member01.getUsername(), PASSWORD);
    InterestDto baseball = insertInterest("야구", member01.getUsername());
    InterestDto running = insertInterest("달리기", member01.getUsername());

    // when
    ExtractableResponse<Response> response =
      RestAssured.given().log().ifValidationFails()
        .spec(spec)
        .when().get("/api/interests")
        .then().log().ifValidationFails()
        .extract();

    //then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    List<InterestDto> list = response.body().jsonPath().getList(".", InterestDto.class);
    assertThat(list).hasSize(2);
    assertThat(list).usingRecursiveComparison().isEqualTo(List.of(baseball, running));
  }

  @Test
  @DisplayName("관심사 화이트리스트 조회에 성공합니다.")
  void getWhitelist_success() {
    // given
    RequestSpecification spec = sigIn(member01.getUsername(), PASSWORD);
    InterestDto baseball = insertInterest("야구", member01.getUsername());
    InterestDto running = insertInterest("달리기", member01.getUsername());

    // when
    ExtractableResponse<Response> response =
        RestAssured.given().log().ifValidationFails()
            .spec(spec)
            .when().get("/api/interests/whitelist")
            .then().log().ifValidationFails()
            .extract();

    //then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    List<String> list = response.body().jsonPath().getList(".", String.class);
    assertThat(list).hasSize(2);
    assertThat(list).usingRecursiveComparison().isEqualTo(Stream.of(baseball, running).map(InterestDto::getName).collect(
        Collectors.toList()));
  }

  @Test
  @DisplayName("id로 관심사를 조회합니다.")
  void readOneInterestByIdSuccessfully() {
    RequestSpecification spec = sigIn(member01.getUsername(), PASSWORD);
    InterestDto swimming = insertInterest("수영", member01.getUsername());

    ExtractableResponse<Response> response =
        RestAssured.given().log().ifValidationFails().spec(spec)
        .when().get("/api/interests/{id}", swimming.getId())
        .then().log().ifValidationFails()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
    assertThat(response.body().as(InterestDto.class))
        .usingRecursiveComparison().isEqualTo(swimming);
  }

  @Test
  @DisplayName("id로 관심사를 삭제합니다.")
  void deleteInterestByIdSuccessfully() {
    RequestSpecification spec = sigIn(member01.getUsername(), PASSWORD);
    InterestDto swimming = insertInterest("수영", member01.getUsername());

    ExtractableResponse<Response> response =
      RestAssured.given().log().ifValidationFails().spec(spec)
        .contentType(ContentType.JSON)
        .when().delete("/api/interests/{id}", swimming.getId())
        .then().log().ifValidationFails()
        .extract();

    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_NO_CONTENT);
  }

  public static InterestDto insertInterest(String interestName, String loginUser) {
    RequestSpecification spec = sigIn(loginUser, PASSWORD);

    CreateInterestRequest request = CreateInterestRequest.builder()
        .interestName(interestName)
        .build();

    return RestAssured.given().log().ifValidationFails().spec(spec)
        .body(request).contentType(ContentType.JSON)
        .when().post("/api/interests")
        .then().log().ifValidationFails()
        .extract().as(InterestDto.class);
  }

  private MemberDto insertMember(String username, String email ,String password) {
    SignUpRequest request = SignUpRequest.builder()
        .username(username)
        .password(password)
        .email(email)
        .build();

    return RestAssured.given().body(request).contentType(ContentType.JSON)
        .when().post("/api/sign-up")
        .then().extract().as(MemberDto.class);
  }

  private static RequestSpecification sigIn(String username, String password) {
    SignInRequest signInRequest = SignInRequest.of(username, password);

    String sessionId = RestAssured.given().log().ifValidationFails()
        .body(signInRequest).contentType(ContentType.JSON)
        .when().post("/api/sign-in")
        .sessionId();
    return new RequestSpecBuilder().setSessionId(sessionId).build();
  }
}
