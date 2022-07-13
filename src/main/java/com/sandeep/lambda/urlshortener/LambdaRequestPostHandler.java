package com.sandeep.lambda.urlshortener;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class LambdaRequestPostHandler implements RequestStreamHandler {

    private DynamoDB dynamoDb;
    private static final String DYNAMODB_TABLE_NAME = "url_shortener";


    public void handleRequest(
            InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {

        initDynamoDbClient();
        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        JSONObject responseJson = new JSONObject();
        ClassLoader loader = LambdaRequestHandler.class.getClassLoader();

        try (InputStream resourceStream = loader.getResourceAsStream("helloPost.html")) {
            JSONObject event = (JSONObject) parser.parse(reader);
            context.getLogger().log("All data " + event.toJSONString());

            String body = (String) event.get("body");
            String longUrl = body.replace("searchbar=", "");
            String shortUrl = generateRandomString();

            persistData(longUrl, shortUrl);
            String htmlContent = new String(resourceStream.readAllBytes(), StandardCharsets.UTF_8);
            htmlContent = htmlContent.replace("{{givenUrl}}", longUrl);
            htmlContent = htmlContent.replace("{{shortenedUrl}}", shortUrl);

            JSONObject headerJson = new JSONObject();
            headerJson.put("Content-Type", "text/html");

            responseJson.put("statusCode", 200);
            responseJson.put("headers", headerJson);
            responseJson.put("body", htmlContent);

            byte[] array = new byte[5];
            new Random().nextBytes(array);
            String generatedString = new String(array,"UTF-8");

        } catch (Exception pex) {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", pex);
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }

    private String generateRandomString() {
        String SALTCHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        StringBuilder salt = new StringBuilder();
        Random rnd = new Random();
        while (salt.length() < 5) {
            int index = (int) (rnd.nextFloat() * SALTCHARS.length());
            salt.append(SALTCHARS.charAt(index));
        }
        String saltStr = salt.toString();
        return saltStr;
    }

    private PutItemOutcome persistData(String longUrl, String shortUrl)
            throws ConditionalCheckFailedException {
        return this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
                .putItem(
                        new PutItemSpec()
                                .withItem(new Item().withString("longUrl", longUrl).withString("shortUrl", shortUrl))
                );
    }

    private void initDynamoDbClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(Regions.US_EAST_1));
        this.dynamoDb = new DynamoDB(client);
    }

}
