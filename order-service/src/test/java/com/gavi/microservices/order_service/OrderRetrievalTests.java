package com.gavi.microservices.order_service;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.testcontainers.mysql.MySQLContainer;

import java.math.BigDecimal;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderRetrievalTests {

    @ServiceConnection
    static MySQLContainer mySQLContainer = new MySQLContainer("mysql:8.3.0");

    @LocalServerPort
    private Integer port;

    @BeforeEach
    void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = port;
    }

    static {
        mySQLContainer.start();
    }

    @Test
    void shouldCreateAndGetAllOrders() {
        String submitOrderJson = """
                {
                     "skuCode": "iphone_15",
                     "price": 1000,
                     "quantity": 1
                }
                """;
        
        // Create order
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .statusCode(201);

        // Get all orders
        RestAssured.given()
                .when()
                .get("/api/order/orders")
                .then()
                .statusCode(200)
                .body("content.size()", Matchers.greaterThan(0));
    }

    @Test
    void shouldGetOrdersWithPagination() {
        String submitOrderJson = """
                {
                     "skuCode": "macbook_pro",
                     "price": 2000,
                     "quantity": 1
                }
                """;
        
        // Create order
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .statusCode(201);

        // Get orders with pagination
        RestAssured.given()
                .queryParam("page", 0)
                .queryParam("size", 5)
                .queryParam("sortBy", "id")
                .queryParam("sortDir", "desc")
                .when()
                .get("/api/order/orders")
                .then()
                .statusCode(200)
                .body("content.size()", Matchers.lessThanOrEqualTo(5))
                .body("size", Matchers.is(5))
                .body("number", Matchers.is(0));
    }

    @Test
    void shouldGetOrdersBySkuCode() {
        String submitOrderJson = """
                {
                     "skuCode": "ipad_air",
                     "price": 600,
                     "quantity": 2
                }
                """;
        
        // Create order
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .statusCode(201);

        // Get orders by SKU code
        RestAssured.given()
                .when()
                .get("/api/order/sku/ipad_air")
                .then()
                .statusCode(200)
                .body("content.size()", Matchers.greaterThan(0))
                .body("content[0].skuCode", Matchers.is("ipad_air"));
    }

    @Test
    void shouldReturn404ForNonExistentOrder() {
        RestAssured.given()
                .when()
                .get("/api/order/non-existent-order")
                .then()
                .statusCode(404);
    }

    @Test
    void shouldGetOrdersWithPriceFilter() {
        String submitOrderJson = """
                {
                     "skuCode": "airpods_pro",
                     "price": 250,
                     "quantity": 1
                }
                """;
        
        // Create order
        RestAssured.given()
                .contentType(ContentType.JSON)
                .body(submitOrderJson)
                .when()
                .post("/api/order")
                .then()
                .statusCode(201);

        // Get orders with price filter
        RestAssured.given()
                .queryParam("minPrice", 200)
                .queryParam("maxPrice", 300)
                .when()
                .get("/api/order/orders")
                .then()
                .statusCode(200)
                .body("content.size()", Matchers.greaterThan(0));
    }
}
