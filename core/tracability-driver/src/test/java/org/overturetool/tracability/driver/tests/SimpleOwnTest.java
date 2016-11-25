package org.overturetool.tracability.driver.tests;

import java.io.IOException;
import java.nio.file.Paths;
import java.text.ParseException;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.sling.commons.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.junit.Test;
import org.overturetool.tracability.driver.Main;
import org.xml.sax.SAXException;

/**
 * Created by kel on 10/11/16.
 */
public class SimpleOwnTest
{

	@Test
	public void test() throws InterruptedException,
			ParserConfigurationException, IOException, JSONException,
			ParseException, GitAPIException, SAXException
	{
		Main.main(new String[] { "-s", "-c", "HEAD", "--dry-run", "-repo",
				Paths.get("../../").toAbsolutePath().normalize().toString() });
	}
}
