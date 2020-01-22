package com.apitests.test;

import io.restassured.http.ContentType;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.File;

import static io.restassured.RestAssured.given;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

public class PostTests {

    private static String URI = "https://api.amediateka.tech/auth/email/sign_in/";
    private static String API_KEY = "eeGaeliYah5veegh";
    private static String CORRECT_MAIL = "testmail001@test.ru";
    private static String CORRECT_PASS = "testtest";

    @AfterMethod
    void tearDown() throws InterruptedException {
        Thread.sleep(1000); // Защита от DDoS
    }

    @DataProvider
    public Object[][] getDataForCorrectMail() {
        return new Object[][]{
                {"testmail001@test.ru", CORRECT_PASS}, // mail is lowercase
                {"TESTMAIL001@TEST.RU", CORRECT_PASS}, // mail is uppercase
                {"tester_MAIL-001@test-001.rus.ru", CORRECT_PASS} // mail with - and _
        };
    }

    @Test(dataProvider = "getDataForCorrectMail")
    public void correctMail(String mail, String pass) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + mail + "\", \"pass\": \"" + pass + "\"}")
                .queryParam("apiKey", API_KEY)
                .when().post(URI)
                .then()
                .statusCode(200)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("TokenInfoAndToken.json")));
    }

    @DataProvider
    public Object[][] getIncorrectBody() {
        String over255String = "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
                "0123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890123456789" +
                "01234567890123456789012345678901234567890123456789012345";
        return new Object[][]{
                {"{\"email\": null, \"pass\": \"" + CORRECT_PASS + "\"}"}, // Email = null
                {"{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": null}"}, // Pass = null
                {"{\"pass\": \"" + CORRECT_PASS + "\"}"}, // Without email
                {"{\"email\": \"" + CORRECT_MAIL + "\"}"}, // Without pass
                {"{\"email\": 123123123, \"pass\": \"" + CORRECT_PASS + "\"}"}, // Mail is not String
                {"{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": 123123}"}, // Pass is not String
                {"{\"email\": \"" + over255String + "\", \"pass\": \"" + CORRECT_PASS + "\"}"}, // Mail over 255
                {"{\"email\": \"01234567890123456789012345678901234567890123456789012345678901234@test.ru\", \"pass\": \"" + CORRECT_PASS + "\"}"}, // Mail local over 64
                {"{\"email\": \"testmail001test.ru\", \"pass\": \"" + CORRECT_PASS + "\"}"}, // Mail without @
                {"{\"email\": \"testmail001@testru\", \"pass\": \"" + CORRECT_PASS + "\"}"}, // Mail without .
                {"{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": \"" + over255String + "\"}"}, // Pass over 255
                {"{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": \"12345\"}"}, // Pass less 6
                {"{\"email\": \"@test.ru\", \"pass\": \"" + CORRECT_PASS + "\"}"}, // Mail without local
                {"{\"email\": \"testmail001\", \"pass\": \"" + CORRECT_PASS + "\"}"} // Mail without domain
        };
    }

    @Test(dataProvider = "getIncorrectBody")
    public void loginWithIncorrectBody(String body) {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .body(body)
                .when().post(URI)
                .then()
                .statusCode(400)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("BadRequest.json")));
    }

    @DataProvider
    public Object[][] getBodyWithEmailDoesNotRegisteredOrNotAcceptPass() {
        return new Object[][]{
                {"{\"email\": \"testmail002@test.ru\", \"pass\": \"123456\"}"}, // Email does not registered
                {"{\"email\": \"testmail001@test.ru\", \"pass\": \"123456\"}"} // Pass not accept
        };
    }

    @Test(dataProvider = "getBodyWithEmailDoesNotRegisteredOrNotAcceptPass")
    public void mailDoesNotExistOrPassNotAccept(String body) {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .body(body)
                .when().post(URI)
                .then()
                .statusCode(404)
                .assertThat().body(matchesJsonSchema(getFileJsonSchema("OtherError.json")));
    }

    @Test
    public void loginWithoutBody() {
        given()
                .contentType(ContentType.JSON)
                .queryParam("apiKey", API_KEY)
                .when().post(URI)
                .then().statusCode(400);
    }

    @Test
    public void loginWithoutApiKey() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\": \"" + CORRECT_MAIL + "\", \"pass\": \"" + CORRECT_PASS + "\"}")
                .when().post(URI)
                .then().statusCode(403);
    }

    private File getFileJsonSchema(String fileName) {
        String jsonSchemaPath = "src/test/resources/jsonschema/";
        return new File(jsonSchemaPath + fileName).getAbsoluteFile();
    }
}