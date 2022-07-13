package com.sandeep.lambda.urlshortener;

import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.ConditionalCheckFailedException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;

public class LambdaRedirectHandler implements RequestStreamHandler {

    private DynamoDB dynamoDb;
    private static final String DYNAMODB_TABLE_NAME = "url_shortener";


    private void initDynamoDbClient() {
        AmazonDynamoDBClient client = new AmazonDynamoDBClient();
        client.setRegion(Region.getRegion(Regions.US_EAST_1));
        dynamoDb = new DynamoDB(client);
    }

    private String getData(String shortUrl) throws ConditionalCheckFailedException {
        return "https://" + this.dynamoDb
                .getTable(DYNAMODB_TABLE_NAME)
                .getItem(
                        new GetItemSpec().withPrimaryKey("shortUrl", shortUrl)
                ).get("longUrl");
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        initDynamoDbClient();
        JSONParser parser = new JSONParser();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

        JSONObject responseJson = new JSONObject();

        try {
            JSONObject event = (JSONObject) parser.parse(reader);
            context.getLogger().log(event.toJSONString());

            String shortUrl = (String) event.get("path");
            shortUrl = shortUrl.replace("/", "");
            String longUrl = getData(shortUrl);

            JSONObject headerJson = new JSONObject();
            headerJson.put("Location", longUrl);

            responseJson.put("statusCode", 302);
            responseJson.put("headers", headerJson);
            responseJson.put("body", "Testing");
        } catch (Exception e) {
            responseJson.put("statusCode", 400);
            responseJson.put("exception", e);
            context.getLogger().log(e.toString());
        }

        OutputStreamWriter writer = new OutputStreamWriter(outputStream, "UTF-8");
        writer.write(responseJson.toString());
        writer.close();
    }
}
