package org.overturetool.tracability.driver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.sling.commons.json.JSONException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.xml.sax.SAXException;

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
			ParserConfigurationException, GitAPIException, JSONException,
			java.text.ParseException
	{
		Options options = new Options();
		Option helpOpt = Option.builder("h").longOpt("help").desc("Show this description").build();

		Option repoPathOpt = Option.builder("repo").longOpt("git-repo-path").desc("A bath to the repo root").hasArg().required().build();
		Option commitOpt = Option.builder("c").longOpt("commit").desc("A commit SHAR1 or branch id. Defaults to HEAD").hasArg().build();

		Option schemeOpt = Option.builder("scheme").hasArg().numberOfArgs(1).argName("github> or <gitlab> or <intocps>").desc("Specify the URL scheme to use.").build();

		Option excludePathPrefixOpt = Option.builder("exclude").hasArg().numberOfArgs(1).argName("path prefix").desc("Prefix of path to exclude.").build();
		Option hostOpt = Option.builder("host").hasArg().numberOfArgs(1).argName("host url").desc("The URL of the host daemon which messages will be send to.").build();

		Option syncOpt = Option.builder("s").longOpt("sync").desc("Perform a full sync of the repository").build();
		Option dryRunOpt = Option.builder("n").longOpt("dry-run").desc("Perform a dry run printing messages to the console").build();

		Option forceOpt = Option.builder("f").longOpt("force").desc("Force override of existing output files").build();
		Option verboseOpt = Option.builder("v").longOpt("verbose").desc("Verbose mode or print diagnostic version info").build();
		Option versionOpt = Option.builder("V").longOpt("version").desc("Show version").build();

		Option vdmOnlyOpt = Option.builder("vdm").desc("Only consider VDM files (*.vdmsl, *.vdmsl, *.vdmrt)").build();
		Option vdmSubModulesOpt = Option.builder("sub").longOpt("submodules").desc("Include submodules").build();
		Option storeOpt = Option.builder("store").hasArg().numberOfArgs(1).argName("path").desc("Store the message with the given path").build();

		options.addOption(helpOpt);
		options.addOption(repoPathOpt);
		options.addOption(commitOpt);
		options.addOption(syncOpt);
		options.addOption(schemeOpt);
		options.addOption(dryRunOpt);
		options.addOption(excludePathPrefixOpt);
		options.addOption(vdmSubModulesOpt);
		options.addOption(hostOpt);
		options.addOption(storeOpt);

		options.addOption(verboseOpt);
		options.addOption(forceOpt);
		options.addOption(versionOpt);
		options.addOption(vdmOnlyOpt);

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
		boolean vdmOnly = cmd.hasOption(vdmOnlyOpt.getOpt());
		boolean vdmSubModulesInclude = cmd.hasOption(vdmSubModulesOpt.getOpt());
		String hostUrl = "http://127.0.0.1:8083";

		boolean dryRun = cmd.hasOption(dryRunOpt.getOpt());

		UrlScheme.SchemeType schemeType = UrlScheme.SchemeType.github;

		if (cmd.hasOption(schemeOpt.getOpt()))
		{
			String scheme = cmd.getOptionValue(schemeOpt.getOpt());

			try
			{
				schemeType = UrlScheme.SchemeType.valueOf(scheme);
			} catch (Exception e)
			{
				System.err.println(
						"Scheme " + scheme + " is not a valid scheme");
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

		if (cmd.hasOption(hostOpt.getOpt()))
		{
			hostUrl = cmd.getOptionValue(hostOpt.getOpt());
		}

		File repoUri = new File(cmd.getOptionValue(repoPathOpt.getOpt()));
		String commit = "HEAD";

		if (cmd.hasOption(commitOpt.getOpt()))
		{
			commit = cmd.getOptionValue(commitOpt.getOpt());
		}

		TraceDriver deriver = new TraceDriver(dryRun, hostUrl, schemeType, Arrays.asList(cmd.getOptionValue(excludePathPrefixOpt.getOpt())), vdmOnly);
		String message = null;
		if (cmd.hasOption(syncOpt.getOpt()))
		{
			// perform full sync
			message = deriver.sync(repoUri, commit, schemeType, vdmSubModulesInclude);
		} else if (cmd.hasOption(commitOpt.getOpt()))
		{
		message=	deriver.sync(repoUri, commit, schemeType, vdmSubModulesInclude, commit);
		}

		if(cmd.hasOption(storeOpt.getOpt()))
		{
			store(message,cmd.getOptionValue(storeOpt.getOpt()));
		}

	}

	private static void store(String message, String path) throws IOException
	{
		FileUtils.write(new File(path),message);
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
