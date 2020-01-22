package com.apitests.test;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

public class DeleteTests {

    private static String URI = "https://api.amediateka.tech/auth/users/";
    private static String API_KEY = "eeGaeliYah5veegh";
    private static String CORRECT_MAIL = "testmail001@test.ru";
    private static String CORRECT_PASS = "testtest";
    private String token = "";
    private String userId = "";

    @BeforeMethod
        // При смене профиля токен авторизации протухает, поэтому получаем новый токен перед каждым методом
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
    public void deleteCurrentProfile() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": \"" + CORRECT_PASS + "\"}")
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .post(URI);
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().delete(URI + userId + "/")
                .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("TokenInfoAndToken.json")));
    }

    @Test
    public void deleteOtherProfile() {
        JsonPath json = given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": \"" + CORRECT_PASS + "\"}")
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().post(URI)
                .jsonPath();
        String userId1 = json.getString("tokenInfo.userId");
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().delete(URI + userId1 + "/")
                .then()
                .statusCode(204);
    }

    @Test
    public void deleteSingleProfile() {
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().delete(URI + userId + "/")
                .then()
                .statusCode(403)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("OtherError.json")));
    }

    @Test
    public void unauthorized() {
        given()
                .queryParam("apiKey", API_KEY)
                .when().delete(URI + userId + "/")
                .then()
                .statusCode(401);
    }

    @Test
    public void userIdIsNotUuid() {
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().delete(URI + "1234/")
                .then()
                .statusCode(400)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("BadRequest.json")));
    }

    @Test
    public void userIdDoesNotExist() {
        given()
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().delete(URI + "12345678-1234-1234-1234-123456789012/")
                .then()
                .statusCode(404)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("OtherError.json")));
    }

    private File getFileJsonSchema(String fileName) {
        String jsonSchemaPath = "src/test/resources/jsonschema/";
        return new File(jsonSchemaPath + fileName).getAbsoluteFile();
    }
}