package org.overturetool.tracability.driver.tests;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Paths;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.sling.commons.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.overturetool.tracability.driver.Main;
import org.xml.sax.SAXException;

/**
 * Created by kel on 10/11/16.
 */
public class SimpleOwnTest
{

	@Test
	public void test() throws InterruptedException, ParserConfigurationException,
			IOException, JSONException, ParseException, GitAPIException,
			SAXException, ProcessingException
	{
		File resultPath =new File( "target/SimpleOwnTest".replace("/", File.separator));
		resultPath.delete();
		resultPath.getParentFile().mkdirs();
		Main.main(new String[] { "-s", "-c", "HEAD", "--dry-run", "-repo",
				Paths.get("../../").toAbsolutePath().normalize().toString() ,"-store",resultPath.getAbsolutePath()});

		Assert.assertTrue("Result file missing",resultPath.exists());
		File schema = new File("src/test/resources/v1.3.2.json".replace("/", File.separator));
		Assert.assertTrue("JSON Schema errors", ValidationUtils.isJsonValid(schema,resultPath));
	}
}
