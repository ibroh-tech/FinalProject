package com.example.sampleapi.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        userRepository.deleteAll();
    }

    @Test
    void createValidUserReturns201() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"John Doe","email":"john.doe@example.com","age":25}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("name", equalTo("John Doe"))
                .body("email", equalTo("john.doe@example.com"))
                .body("age", equalTo(25));
    }

    @Test
    void blankNameReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"   ","email":"blank.name@example.com","age":25}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400);
    }

    @Test
    void invalidEmailReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Invalid Email","email":"not-an-email","age":25}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400);
    }

    @Test
    void ageOutsideRangeReturns400() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Too Young","email":"too.young@example.com","age":0}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Too Old","email":"too.old@example.com","age":121}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(400);
    }

    @Test
    void duplicateEmailReturns409() {
        createUser("Original User", "duplicate@example.com", 30);

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Second User","email":"Duplicate@Example.com","age":31}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(409);
    }

    private void createUser(String name, String email, int age) {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"%s","email":"%s","age":%d}
                        """.formatted(name, email, age))
        .when()
                .post("/api/users")
        .then()
                .statusCode(201);
    }
}
