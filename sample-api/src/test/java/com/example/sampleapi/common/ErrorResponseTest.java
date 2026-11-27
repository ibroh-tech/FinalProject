package com.example.sampleapi.common;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorResponseTest {

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Test
    void unknownRouteReturnsConsistent404() {
        given()
        .when()
                .get("/api/does-not-exist")
        .then()
                .statusCode(404)
                .body("timestamp", notNullValue())
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", equalTo("Resource not found"))
                .body("path", equalTo("/api/does-not-exist"))
                .body("details", notNullValue());
    }

    @Test
    void unsupportedMethodReturnsConsistent405() {
        given()
        .when()
                .patch("/api/users")
        .then()
                .statusCode(405)
                .body("timestamp", notNullValue())
                .body("status", equalTo(405))
                .body("error", equalTo("Method Not Allowed"))
                .body("path", equalTo("/api/users"))
                .body("details", notNullValue());
    }
}
