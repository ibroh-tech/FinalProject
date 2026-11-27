package com.example.sampleapi.product;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        productRepository.deleteAll();
    }

    @AfterEach
    void cleanUp() {
        productRepository.deleteAll();
    }

    @Test
    void createReadUpdateDeleteFlow() {
        long id = given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Keyboard","description":"Mechanical keyboard","price":49.99,"stock":10}
                        """)
        .when()
                .post("/api/products")
        .then()
                .statusCode(201)
                .body("name", equalTo("Keyboard"))
                .body("price", equalTo(49.99f))
                .body("stock", equalTo(10))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
        .when()
                .get("/api/products/{id}", id)
        .then()
                .statusCode(200)
                .body("name", equalTo("Keyboard"))
                .body("description", equalTo("Mechanical keyboard"));

        given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name":"Compact Keyboard","description":"","price":39.99,"stock":5}
                        """)
        .when()
                .put("/api/products/{id}", id)
        .then()
                .statusCode(200)
                .body("name", equalTo("Compact Keyboard"))
                .body("description", nullValue())
                .body("price", equalTo(39.99f))
                .body("stock", equalTo(5));

        given()
        .when()
                .delete("/api/products/{id}", id)
        .then()
                .statusCode(204);

        given()
        .when()
                .get("/api/products/{id}", id)
        .then()
                .statusCode(404);
    }
}
