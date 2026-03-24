package com.contractdetector.tests;

import com.contractdetector.capture.SchemaCaptureFilter;
import com.contractdetector.capture.SchemaCaptureService;
import com.contractdetector.capture.SchemaSampleRepository;
import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
public class ApiContractTest {

    @Autowired
    private SchemaCaptureService schemaCaptureService;

    @Autowired
    private SchemaSampleRepository schemaSampleRepository;

    @BeforeEach
    public void setup() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 3000;
        
        RestAssured.replaceFiltersWith(new SchemaCaptureFilter(schemaCaptureService));
        
        schemaSampleRepository.deleteAll();
    }

    @Test
    public void testGetUsersCapturesSchema() throws InterruptedException {
        SchemaCaptureFilter.setTestContext("ApiContractTest", "testGetUsersCapturesSchema");

        given()
            .when()
            .get("/users")
            .then()
            .statusCode(200)
            .body("name",everyItem(notNullValue()));

        SchemaCaptureFilter.clearTestContext();
    }

    @Test
    public void testGetProductsCapturesSchema() throws InterruptedException {
        SchemaCaptureFilter.setTestContext("ApiContractTest", "testGetProductsCapturesSchema");

        given()
            .when()
            .get("/products")
            .then()
            .statusCode(200)
            .body("$", not(empty()));

        SchemaCaptureFilter.clearTestContext();
    }
}
