package com.example.driveexample;

import java.io.IOException;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;

public class ReqInitializer implements HttpRequestInitializer {
	protected String token;
	
	public ReqInitializer(String tok)
	{
		token=tok;
	}
	
	@Override
	public void initialize(HttpRequest req) throws IOException {
		HttpHeaders headers = req.getHeaders();
		headers.put("Authorization", "Bearer "+token);

	}

}
