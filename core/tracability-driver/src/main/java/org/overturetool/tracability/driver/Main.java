package org.overturetool.tracability.driver;

import org.apache.commons.cli.*;
import org.apache.sling.commons.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Main
{

	static boolean checkRequiredOptions(CommandLine cmd, Option... opt)
	{
		for (Option option : opt)
		{
			if (!cmd.hasOption(option.getOpt()))
			{
				System.err.println(
						"Missing required option: " + option.getOpt());
				return false;
			}
		}
		return true;
	}

	public static void main(String[] args)
			throws IOException, InterruptedException, SAXException,
			ParserConfigurationException, GitAPIException, JSONException
	{
		Options options = new Options();
		Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();

		Option repoPathOpt = Option.builder("repo").longOpt("git-repo-path").desc("A bath to the repo root").hasArg().required().build();
		Option commitOpt = Option.builder("c").longOpt("commit").desc("A commit SHAR1 or branch id").hasArg().required().build();

		Option schemeOpt = Option.builder("scheme").hasArg().numberOfArgs(1).argName("github> or <gitlab> or <intocps>").desc("Specify the URL scheme to use.").build();

		Option syncOpt = Option.builder("s").longOpt("sync").desc("Perform a full sync of the repository").build();
		Option dryRunOpt = Option.builder("n").longOpt("dry-run").desc("Perform a dry run printing messages to the console").build();

		Option forceOpt = Option.builder("f").longOpt("force").desc("Force override of existing output files").build();
		Option verboseOpt = Option.builder("v").longOpt("verbose").desc("Verbose mode or print diagnostic version info").build();
		Option versionOpt = Option.builder("V").longOpt("version").desc("Show version").build();

		options.addOption(helpOpt);
		options.addOption(repoPathOpt);
		options.addOption(commitOpt);
		options.addOption(syncOpt);
		options.addOption(schemeOpt);
		options.addOption(dryRunOpt);

		options.addOption(verboseOpt);
		options.addOption(forceOpt);
		options.addOption(versionOpt);

		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = null;
		try
		{
			cmd = parser.parse(options, args);
		} catch (ParseException e1)
		{
			System.err.println("Parsing failed. Reason: " + e1.getMessage());
			showHelp(options);
			return;
		}

		if (cmd.hasOption(helpOpt.getOpt()))
		{
			showHelp(options);
			return;
		}

		// check option combinations

		boolean force = cmd.hasOption(forceOpt.getOpt());
		boolean verbose = cmd.hasOption(verboseOpt.getOpt());
		boolean version = cmd.hasOption(versionOpt.getOpt());

		boolean dryRun = cmd.hasOption(dryRunOpt.getOpt());

		UrlScheme.SchemeType schemeType = UrlScheme.SchemeType.github;

		if(cmd.hasOption(schemeOpt.getOpt()))
		{
			String scheme = cmd.getOptionValue(schemeOpt.getOpt());

			try
			{
				schemeType = UrlScheme.SchemeType.valueOf(scheme);
			}catch(Exception e)
			{
				System.err.println("Scheme "+ scheme +" is not a valid scheme");
				return;
			}
		}

		if (verbose || version)
		{
			showVersion();
			if (version)
			{
				return;
			}
		}

		File repoUri = new File(cmd.getOptionValue(repoPathOpt.getOpt())+File.separatorChar+".git");
		String commit = cmd.getOptionValue(commitOpt.getOpt());

		TraceDriver deriver = new TraceDriver(dryRun,schemeType);
		if (cmd.hasOption(syncOpt.getOpt()))
		{
			//perform full sync
			deriver.fullSync(repoUri,commit, schemeType);
		}

	}

	private static void showVersion()
	{
		try
		{
			Properties prop = new Properties();
			InputStream coeProp = Main.class.getResourceAsStream("/tracability-driver.properties");
			prop.load(coeProp);
			System.out.println("Tool: " + prop.getProperty("artifactId"));
			System.out.println("Version: " + prop.getProperty("version"));
		} catch (Exception e)
		{
		}

	}

	public static void showHelp(Options options)
	{
		HelpFormatter formatter = new HelpFormatter();
		formatter.printHelp("fmu-import-export", options);
	}

}
