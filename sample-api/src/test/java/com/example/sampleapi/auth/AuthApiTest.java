package com.example.sampleapi.auth;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.ValidatableResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthApiTest {

    private static final String PASSWORD = "Password123!";

    @LocalServerPort
    private int port;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AuthSessionRepository authSessionRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        cleanDatabase();
    }

    @AfterEach
    void cleanUp() {
        cleanDatabase();
    }

    @Test
    void registerReturnsTokensAndStoresLowercaseEmail() {
        Tokens tokens = register("New.User@Example.com");

        assertTrue(accountRepository.findByEmail("new.user@example.com").isPresent());
        assertEquals(1, authSessionRepository.count());
        assertNotEquals(tokens.accessToken(), tokens.refreshToken());
    }

    @Test
    void duplicateRegistrationReturns409WithErrorShape() {
        register("duplicate@example.com");

        given()
                .contentType(ContentType.JSON)
                .body(registerJson("Other User", "DUPLICATE@example.com", PASSWORD))
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("path", equalTo("/api/auth/register"))
                .body("details", notNullValue());
    }

    @Test
    void loginWithNormalisedEmailReturnsTokens() {
        register("Login.User@Example.com");

        given()
                .contentType(ContentType.JSON)
                .body(loginJson("login.user@example.com", PASSWORD))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresInSeconds", equalTo(900));
    }

    @Test
    void wrongPasswordReturns401WithErrorShape() {
        register("wrong.password@example.com");

        given()
                .contentType(ContentType.JSON)
                .body(loginJson("wrong.password@example.com", "WrongPassword123!"))
        .when()
                .post("/api/auth/login")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("error", equalTo("Unauthorized"))
                .body("message", equalTo("Invalid email or password"))
                .body("path", equalTo("/api/auth/login"))
                .body("details", notNullValue());
    }

    @Test
    void refreshRotatesTokensAndRejectsOldRefreshToken() {
        Tokens first = register("refresh@example.com");

        ValidatableResponse refreshResponse = given()
                .contentType(ContentType.JSON)
                .body(refreshJson(first.refreshToken()))
        .when()
                .post("/api/auth/refresh")
        .then()
                .statusCode(200)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue());

        Tokens second = new Tokens(
                refreshResponse.extract().path("accessToken"),
                refreshResponse.extract().path("refreshToken"));

        assertNotEquals(first.accessToken(), second.accessToken());
        assertNotEquals(first.refreshToken(), second.refreshToken());

        given()
                .contentType(ContentType.JSON)
                .body(refreshJson(first.refreshToken()))
        .when()
                .post("/api/auth/refresh")
        .then()
                .statusCode(401)
                .body("message", equalTo("Invalid or expired refresh token"));
    }

    @Test
    void invalidRefreshTokenReturns401() {
        given()
                .contentType(ContentType.JSON)
                .body(refreshJson("not-a-real-token"))
        .when()
                .post("/api/auth/refresh")
        .then()
                .statusCode(401)
                .body("status", equalTo(401))
                .body("message", containsString("refresh token"));
    }

    @Test
    void shortPasswordReturns400WithDetails() {
        given()
                .contentType(ContentType.JSON)
                .body(registerJson("Short Password", "short@example.com", "short"))
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("Validation failed"))
                .body("details", hasItem("password: password must be between 8 and 72 characters"));
    }

    private Tokens register(String email) {
        ValidatableResponse response = given()
                .contentType(ContentType.JSON)
                .body(registerJson("Test User", email, PASSWORD))
        .when()
                .post("/api/auth/register")
        .then()
                .statusCode(201)
                .body("accessToken", notNullValue())
                .body("refreshToken", notNullValue())
                .body("tokenType", equalTo("Bearer"))
                .body("expiresInSeconds", equalTo(900));

        return new Tokens(
                response.extract().path("accessToken"),
                response.extract().path("refreshToken"));
    }

    private static String registerJson(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s"}
                """.formatted(name, email, password);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}
                """.formatted(email, password);
    }

    private static String refreshJson(String refreshToken) {
        return """
                {"refreshToken":"%s"}
                """.formatted(refreshToken);
    }

    private void cleanDatabase() {
        authSessionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    private record Tokens(String accessToken, String refreshToken) {
    }
}
