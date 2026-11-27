package com.example.sampleapi.order;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.example.sampleapi.product.Product;
import com.example.sampleapi.product.ProductRepository;
import com.example.sampleapi.user.User;
import com.example.sampleapi.user.UserRepository;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderApiTest {

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    private Long userId;
    private Long productId;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
        cleanDatabase();

        User user = userRepository.save(new User("Order User", "order.user@example.com", 30));
        Product product = productRepository.save(
                new Product("Keyboard", "Mechanical keyboard", new BigDecimal("49.99"), 10));
        userId = user.getId();
        productId = product.getId();
    }

    @AfterEach
    void cleanUp() {
        cleanDatabase();
    }

    @Test
    void createOrderReturnsPendingOrder() {
        long id = given()
                .contentType(ContentType.JSON)
                .body(orderJson(3))
        .when()
                .post("/api/orders")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("quantity", equalTo(3))
                .body("unitPrice", equalTo(49.99f))
                .body("totalPrice", equalTo(149.97f))
                .body("status", equalTo("PENDING"))
                .extract()
                .jsonPath()
                .getLong("id");

        given()
        .when()
                .get("/api/orders/{id}", id)
        .then()
                .statusCode(200)
                .body("quantity", equalTo(3))
                .body("status", equalTo("PENDING"));
    }

    @Test
    void insufficientStockReturns409WithErrorShape() {
        given()
                .contentType(ContentType.JSON)
                .body(orderJson(11))
        .when()
                .post("/api/orders")
        .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("error", equalTo("Conflict"))
                .body("message", containsString("exceeds available stock"))
                .body("path", equalTo("/api/orders"))
                .body("details", notNullValue())
                .body("timestamp", notNullValue());
    }

    @Test
    void cancelPendingOrderReturnsCancelled() {
        long id = createOrder(2);

        given()
        .when()
                .post("/api/orders/{id}/cancel", id)
        .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));

        given()
        .when()
                .post("/api/orders/{id}/cancel", id)
        .then()
                .statusCode(409)
                .body("message", containsString("already cancelled"));
    }

    @Test
    void cannotCancelShippedOrder() {
        long id = createOrder(3);

        updateOrder(id, 3, "PAID");
        updateOrder(id, 3, "SHIPPED");

        given()
        .when()
                .post("/api/orders/{id}/cancel", id)
        .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("message", containsString("shipped"));

        given()
                .contentType(ContentType.JSON)
                .body(orderUpdateJson(4, "SHIPPED"))
        .when()
                .put("/api/orders/{id}", id)
        .then()
                .statusCode(409)
                .body("message", containsString("cannot be modified"));
    }

    @Test
    void quantityCannotChangeAfterPayment() {
        long id = createOrder(3);

        updateOrder(id, 4, "PAID");

        given()
                .contentType(ContentType.JSON)
                .body(orderUpdateJson(5, "PAID"))
        .when()
                .put("/api/orders/{id}", id)
        .then()
                .statusCode(409)
                .body("message", containsString("Quantity can only be changed"));
    }

    @Test
    void invalidStatusTransitionReturns409() {
        long id = createOrder(3);

        given()
                .contentType(ContentType.JSON)
                .body(orderUpdateJson(3, "SHIPPED"))
        .when()
                .put("/api/orders/{id}", id)
        .then()
                .statusCode(409)
                .body("message", containsString("Cannot change order status from PENDING to SHIPPED"));
    }

    @Test
    void missingOrderReturns404WithErrorShape() {
        given()
        .when()
                .get("/api/orders/999999")
        .then()
                .statusCode(404)
                .body("status", equalTo(404))
                .body("error", equalTo("Not Found"))
                .body("message", containsString("Order not found"))
                .body("path", equalTo("/api/orders/999999"))
                .body("details", notNullValue());
    }

    @Test
    void invalidQuantityReturns400WithDetails() {
        given()
                .contentType(ContentType.JSON)
                .body(orderJson(0))
        .when()
                .post("/api/orders")
        .then()
                .statusCode(400)
                .body("status", equalTo(400))
                .body("error", equalTo("Bad Request"))
                .body("message", equalTo("Validation failed"))
                .body("details", hasItem("quantity: quantity must be at least 1"));
    }

    private long createOrder(int quantity) {
        return given()
                .contentType(ContentType.JSON)
                .body(orderJson(quantity))
        .when()
                .post("/api/orders")
        .then()
                .statusCode(201)
                .extract()
                .jsonPath()
                .getLong("id");
    }

    private void updateOrder(long id, int quantity, String status) {
        given()
                .contentType(ContentType.JSON)
                .body(orderUpdateJson(quantity, status))
        .when()
                .put("/api/orders/{id}", id)
        .then()
                .statusCode(200)
                .body("status", equalTo(status));
    }

    private String orderJson(int quantity) {
        return """
                {"userId":%d,"productId":%d,"quantity":%d}
                """.formatted(userId, productId, quantity);
    }

    private static String orderUpdateJson(int quantity, String status) {
        return """
                {"quantity":%d,"status":"%s"}
                """.formatted(quantity, status);
    }

    private void cleanDatabase() {
        orderRepository.deleteAll();
        userRepository.deleteAll();
        productRepository.deleteAll();
    }
}
