package com.apitests.test;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

public class GetTests {

    private static String URI = "https://api.amediateka.tech/auth/users/";
    private static String API_KEY = "eeGaeliYah5veegh";
    private static String CORRECT_MAIL = "testmail001@test.ru";
    private static String CORRECT_PASS = "testtest";
    private String token = "";
    private String userId = "";

    @BeforeClass
    void setUp() {
        JsonPath json = given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": \"" + CORRECT_PASS + "\"}")
                .queryParam("apiKey", API_KEY)
                .when().post("https://api.amediateka.tech/auth/email/sign_in/").jsonPath();
        token = json.getString("token");
        userId = json.getString("tokenInfo.userId");
    }

    @AfterMethod
    void tearDown() throws InterruptedException {
        Thread.sleep(1000); // Защита от DDoS
    }

    @Test
    public void correctProfile() {
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().get(URI + userId + "/")
                .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("UserInfo.json")));
    }

    @Test
    public void unauthorized() {
        given()
                .queryParam("apiKey", API_KEY)
                .when().get(URI + userId + "/")
                .then()
                .statusCode(401);
    }

    @Test
    public void userIdIsNotUuid() {
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().get(URI + "1234/")
                .then()
                .statusCode(400)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("BadRequest.json")));
    }

    @Test
    public void userIdDoesNotExist() {
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().get(URI + "12345678-1234-1234-1234-123456789012/")
                .then()
                .statusCode(404)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("OtherError.json")));
    }

    private File getFileJsonSchema(String fileName) {
        String jsonSchemaPath = "src/test/resources/jsonschema/";
        return new File(jsonSchemaPath + fileName).getAbsoluteFile();
    }
}