package com.sandeep.lambda.urlshortener;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class LambdaRequestHandler implements RequestStreamHandler{

    @Override
    public void handleRequest(InputStream inputStream, OutputStream os, Context context) throws IOException {
        try {
            ClassLoader loader = LambdaRequestHandler.class.getClassLoader();
            try(InputStream resourceStream = loader.getResourceAsStream("hello.html")) {
                os.write(IOUtils.toByteArray(resourceStream));
            }
        }catch(Exception ex) {
            os.write("Error in generating output.".getBytes());
        }
    }
}
