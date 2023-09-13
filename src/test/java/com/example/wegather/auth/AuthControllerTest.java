package com.example.wegather.auth;


import static com.example.wegather.global.vo.MemberType.ROLE_USER;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.wegather.auth.dto.SignInRequest;
import com.example.wegather.auth.dto.SignUpRequest;
import com.example.wegather.global.vo.MemberType;
import com.example.wegather.integration.IntegrationTest;
import com.example.wegather.member.domain.MemberRepository;
import com.example.wegather.member.dto.MemberDto;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AuthControllerTest extends IntegrationTest {

  @Autowired
  MemberRepository memberRepository;

  SignUpRequest signUpRequest = SignUpRequest.builder()
      .username("test01")
      .password("password")
      .name("테스트유져")
      .phoneNumber("010-1234-1234")
      .memberType(MemberType.ROLE_USER)
      .build();

  @Test
  @DisplayName("회원가입을 합니다.")
  void signUp_success() {
    // when
    ExtractableResponse<Response> response = requestSignUp(signUpRequest);

    //then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_CREATED);
    MemberDto memberDto = response.body().as(MemberDto.class);
    assertThat(memberDto)
        .usingRecursiveComparison()
        .ignoringFields("id", "profileImage", "address", "interests")
        .ignoringActualNullFields()
        .isEqualTo(signUpRequest);
  }

  @Test
  @DisplayName("username 이 이미 존재하는 경우 예외를 던집니다.")
  void signUp_fail_when_username_already_exists() {
    // given
    requestSignUp(signUpRequest);

    // when
    ExtractableResponse<Response> response = requestSignUp(signUpRequest);

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_BAD_REQUEST);
  }

  private static ExtractableResponse<Response> requestSignUp(
      SignUpRequest signUpRequest) {
    ExtractableResponse<Response> response = RestAssured.given().log().all()
        .body(signUpRequest).contentType(ContentType.JSON)
        .when().post("/sign-up")
        .then().log().all()
        .extract();
    return response;
  }

  @Test
  @DisplayName("로그인을 성공합니다.")
  void signIn_success() {
    // given
    requestSignUp(signUpRequest);
    SignInRequest signInRequest =
        SignInRequest.of(signUpRequest.getUsername(), signUpRequest.getPassword());

    // when
    ExtractableResponse<Response> response = requestSignIn(signInRequest);

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_OK);
  }

  @Test
  @DisplayName("username 을 찾을 수 없어 로그인에 실패합니다.")
  void signIn_fail_because_username_not_found() {
    // given
    requestSignUp(signUpRequest);
    SignInRequest signInRequest =
        SignInRequest.of("notFoundUsername", signUpRequest.getPassword());

    // when
    ExtractableResponse<Response> response = requestSignIn(signInRequest);

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
  }

  @Test
  @DisplayName("password 가 로그인에 실패합니다.")
  void signIn_fail_because_password_invalid() {
    // given
    requestSignUp(signUpRequest);
    SignInRequest signInRequest =
        SignInRequest.of(signUpRequest.getUsername(), "fail_password");

    // when
    ExtractableResponse<Response> response = requestSignIn(signInRequest);

    // then
    assertThat(response.statusCode()).isEqualTo(HttpStatus.SC_UNAUTHORIZED);
  }

  private ExtractableResponse<Response> requestSignIn(SignInRequest signInRequest) {
    return RestAssured.given().log().all()
        .body(signInRequest).contentType(ContentType.JSON)
        .when().post("/sign-in")
        .then().log().all()
        .extract();
  }

}
