package com.apitests.test;

import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

public class PutTests {

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
    public void editProfileWithoutParams() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().put(URI + userId + "/")
                .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("UserInfo.json")));
    }

    @DataProvider
    public Object[][] getProfileName() {
        return new Object[][]{
                {"1", "1"}, // min symbol
                {"012345678901234567890123", "012345678901234567890123"}, // max symbols
                {"new profile", "new profile"}, // with space
                {" profile", "profile"}, // with space before name
                {"profile ", "profile"} // with space after name
        };
    }

    @Test(dataProvider = "getProfileName")
    public void correctProfileName(String inputName, String outputName) {
        Response response = given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .body("{\"userName\": \"" + inputName + "\", \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar9.jpg\"}")
                .when().put(URI + userId + "/");
        String userName = response
                .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("UserInfo.json")))
                .assertThat().extract().jsonPath().getString("userName");
        Assert.assertEquals(outputName, userName);
    }

    @Test
    public void unauthorized() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .when().put(URI + userId + "/")
                .then()
                .statusCode(401);
    }

    @DataProvider
    public Object[][] getIncorrectBody() {
        return new Object[][]{
                {"{\"userName\": null, \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar9.jpg\"}"}, // userName = null
                {"{\"userName\": \"\", \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar9.jpg\"}"}, // userName is empty
                {"{\"userName\": \" \", \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar9.jpg\"}"}, // userName is space
                {"{\"userName\": 123, \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar9.jpg\"}"}, // userName is not String
                {"{\"userName\": \"0123456789012345678901234\", \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar9.jpg\"}"}, // userName over 24
                {"{\"userName\": \"profile\", \"avatarUrl\": null}"}, // avatarUrl = null
                {"{\"userName\": \"profile\", \"avatarUrl\": \"\"}"}, // avatarUrl is empty
                {"{\"userName\": \"profile\", \"avatarUrl\": \" \"}"}, // avatarUrl is space
                {"{\"userName\": \"profile\", \"avatarUrl\": \"https://s80658.cdn.ngenix.net/i/avatars/avatar999.jpg\"}"} // avatarUrl does not exist
        };
    }

    @Test(dataProvider = "getIncorrectBody")
    public void incorrectBody(String body) {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .body(body)
                .when().put(URI + userId + "/")
                .then()
                .statusCode(400)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("BadRequest.json")));
    }

    @Test
    public void userIdIsNotUuid() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().put(URI + "1234/")
                .then()
                .statusCode(400)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("BadRequest.json")));
    }

    @Test
    public void userIdDoesNotExist() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .queryParam("token", token)
                .when().put(URI + "12345678-1234-1234-1234-123456789012/")
                .then()
                .statusCode(404)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("OtherError.json")));
    }

    private File getFileJsonSchema(String fileName) {
        String jsonSchemaPath = "src/test/resources/jsonschema/";
        return new File(jsonSchemaPath + fileName).getAbsoluteFile();
    }
}