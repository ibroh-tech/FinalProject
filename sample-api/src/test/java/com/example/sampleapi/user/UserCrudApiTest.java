package com.example.sampleapi.user;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserCrudApiTest {

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
    void createReadUpdateDeleteFlow() {
        long id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"John Doe","email":"john.doe@example.com","age":25}
                        """)
        .when()
                .post("/api/users")
        .then()
                .statusCode(201)
                .body("email", equalTo("john.doe@example.com"))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
        .when()
                .get("/api/users/{id}", id)
        .then()
                .statusCode(200)
                .body("name", equalTo("John Doe"))
                .body("age", equalTo(25));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"John Updated","email":"john.updated@example.com","age":30}
                        """)
        .when()
                .put("/api/users/{id}", id)
        .then()
                .statusCode(200)
                .body("id", equalTo((int) id))
                .body("name", equalTo("John Updated"))
                .body("email", equalTo("john.updated@example.com"))
                .body("age", equalTo(30));

        given()
        .when()
                .delete("/api/users/{id}", id)
        .then()
                .statusCode(204);

        given()
        .when()
                .get("/api/users/{id}", id)
        .then()
                .statusCode(404);
    }
}
