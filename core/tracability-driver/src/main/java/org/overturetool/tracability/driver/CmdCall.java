package org.overturetool.tracability.driver;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kel on 04/11/16.
 */
public class CmdCall
{
	final static Logger logger = LoggerFactory.getLogger(CmdCall.class);


	public static List<String> call(File workingDir, String... args)
			throws IOException, InterruptedException
	{
		return call(true,workingDir,args);
	}

	public static List<String> call(boolean showFailures, File workingDir, String... args)
			throws IOException, InterruptedException
	{
		List<String> output = new Vector<>();
		ProcessBuilder pb = new ProcessBuilder();
		pb.command(args);

		pb.directory(workingDir);
		logger.trace("Executing '{}' in {}", String.join(" ", args), workingDir.getAbsolutePath());
		Process p = pb.start();

		if(showFailures)
		{
			Thread t = new Thread(() ->
			{
				{
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
					String line = null;
					try
					{
						while ((line = reader.readLine()) != null)
						{
							System.err.println(line);
						}
					} catch (IOException e)
					{
						e.printStackTrace();
					}
				}

			});
			t.setDaemon(true);
			t.start();
		}

		BufferedReader readerOut = new BufferedReader(new InputStreamReader(p.getInputStream()));
		String lineOut;
		while ((lineOut = readerOut.readLine()) != null)
		{
			output.add(lineOut);
		}

		if (p.waitFor() != 0)
		{
			if(showFailures)
			{
				BufferedReader reader = new BufferedReader(new InputStreamReader(p.getErrorStream()));
				String line;
				while ((line = reader.readLine()) != null)
				{
					System.err.println(line);
				}
			}
			return null;
		}

		return output;
	}
}