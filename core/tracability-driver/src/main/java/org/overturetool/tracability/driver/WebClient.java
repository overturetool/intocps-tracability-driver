package org.overturetool.tracability.driver;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

/**
 * Created by kel on 03/11/16.
 */
public class WebClient
{

	public static void post(String url, String... data) throws IOException
	{
		HttpClient httpClient = HttpClientBuilder.create().build();

		try
		{

			for (String message : data)
			{
				HttpPost request = new HttpPost(url);
				StringEntity params = new StringEntity(message);
				request.addHeader("content-type", "application/json");
				request.setEntity(params);
				HttpResponse response = httpClient.execute(request);

				HttpEntity entity = response.getEntity();

				String responseString = EntityUtils.toString(entity, "UTF-8");

				System.out.println(responseString);

			}

			// handle response here...
		} finally
		{
			httpClient.getConnectionManager().shutdown(); // Deprecated
		}
	}
}
