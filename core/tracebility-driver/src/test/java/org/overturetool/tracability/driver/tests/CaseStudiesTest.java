package org.overturetool.tracability.driver.tests;

import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import org.apache.sling.commons.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Assert;
import org.junit.Test;
import org.overturetool.tracability.driver.Main;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;

/**
 * Created by kel on 24/08/2017.
 */
public class CaseStudiesTest
{

	String getRepoPath(String name)
	{
		return Paths.get("src/test/resources/".replace("/", File.separator)+name).toAbsolutePath().normalize().toString();
	}

	File runTraceTest(String name)
			throws InterruptedException, ParserConfigurationException,
			IOException, JSONException, ParseException, GitAPIException,
			SAXException, ProcessingException
	{
		File resultPath =new File( "target/CaseStudiesTest/".replace("/", File.separator)+name+".json");
		resultPath.delete();
		resultPath.getParentFile().mkdirs();
		Main.main(new String[] { "-s", "-c", "HEAD", "--dry-run","-vdm", "-exclude" ,"SysML", "-repo",
				getRepoPath(name) ,"-store",resultPath.getAbsolutePath()});

		Assert.assertTrue("Result file missing",resultPath.exists());
		File schema = new File("src/test/resources/v1.4.json".replace("/", File.separator));
		Assert.assertTrue("JSON Schema errors", ValidationUtils.isJsonValid(schema,resultPath));
		return resultPath;
	}
	@Test
	public void singleWatertankTest() throws InterruptedException,
			ParserConfigurationException, IOException, JSONException,
			ParseException, GitAPIException, SAXException, ProcessingException
	{
		runTraceTest("case-study_single_watertank");
	}

	@Test
	public void lineFollowerRobotTest() throws InterruptedException,
			ParserConfigurationException, IOException, JSONException,
			ParseException, GitAPIException, SAXException, ProcessingException
	{
		runTraceTest("case-study_line_follower_robot");
	}

	@Test
	public void FcuTest() throws InterruptedException,
			ParserConfigurationException, IOException, JSONException,
			ParseException, GitAPIException, SAXException, ProcessingException
	{
		runTraceTest("case-study_fcu");
	}


}
