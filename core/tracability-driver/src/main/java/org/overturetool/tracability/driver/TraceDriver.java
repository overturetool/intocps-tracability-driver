package org.overturetool.tracability.driver;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.overture.ast.definitions.SClassDefinition;
import org.overture.ast.lex.Dialect;
import org.overture.config.Settings;
import org.overture.parser.util.ParserUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kel on 03/11/16.
 */
public class TraceDriver
{
	final static Logger logger = LoggerFactory.getLogger(TraceDriver.class);
	private boolean dryRun;
	UrlScheme.SchemeType urlScheme;
	final List<String> excludePrefix;
	boolean vdmOnly = false;
	final String hostUrl;

	public TraceDriver(boolean dryRun, String hostUrl,
			UrlScheme.SchemeType urlScheme, List<String> excludePathPrefix,
			boolean vdmOnly)
	{
		this.dryRun = dryRun;
		this.hostUrl = hostUrl;
		this.urlScheme = urlScheme;
		this.excludePrefix = excludePathPrefix;
		this.vdmOnly = vdmOnly;
	}

	public void sync(File repoUri, String commitId,
			UrlScheme.SchemeType scheme, boolean vdmSubModulesInclude,
			String commit) throws IOException, GitAPIException, JSONException,
			ParseException
	{
		internalSync(repoUri, commitId, scheme, vdmSubModulesInclude, Arrays.asList(commit));
	}

	public void sync(File repoUri, String commitId,
			UrlScheme.SchemeType scheme, boolean vdmSubModulesInclude)
			throws IOException, GitAPIException, JSONException, ParseException,
			InterruptedException
	{
		final IGitRepo cmdGit = new CmdGitRepo(repoUri);
		List<String> refs = cmdGit.getCommitHistory(commitId);
		internalSync(repoUri, commitId, scheme, vdmSubModulesInclude, refs);
	}

	private void internalSync(File repoUri, String commitId,
			UrlScheme.SchemeType scheme, boolean vdmSubModulesInclude,
			List<String> commits) throws IOException, GitAPIException,
			JSONException, ParseException
	{

		final IGitRepo cmdGit = new CmdGitRepo(repoUri);
		try
		{
			final IStructureProvider sp = (repoCtxt, parent, obj) -> {
				List<JSONObject> list = new Vector<>();

				if (vdmSubModulesInclude && vdmOnly)
				{
					File source = new File(repoUri.getParent(), parent.replace('/', File.separatorChar));
					Settings.dialect = Dialect.VDM_PP;
					try
					{
						ParserUtil.ParserResult<List<SClassDefinition>> r = ParserUtil.parseOo(source);

						for (SClassDefinition classDefinition : r.result)
						{
							JSONObject child = new JSONObject();
							logger.trace("\t\t\tCreating entry for: {}", classDefinition.getName().getName());
							child.put(IntoTraceProtocol.rdf_about, obj.get(IntoTraceProtocol.rdf_about)
									+ ":" + classDefinition.getName().getName());
							child.put("path", obj.get("path"));
							child.put("hash", obj.get("hash")); // TODO what is a hash
							child.put("comment", obj.get("comment"));
							child.put("type", "definition");
							list.add(child);
						}
					} catch (Exception e)
					{
						System.err.println("Failure in get children parsing");
						e.printStackTrace();
					}
				}

				return list;
			};

			IGitRepoContext gitCtxt = new GitCtxt(commitId, scheme);

			IntoTraceProtocol.ITMessage res = new IntoTraceProtocol.ITMessage();

			for (String ref : commits)
			{
				gitCtxt = gitCtxt.changeCommit(ref);

				logger.trace("Proceeding commit: {}", gitCtxt.getCommit());

				boolean first = true;

				IntoTraceProtocol.ITMessage agentMsg = null;
				IntoTraceProtocol.ITMessage activity = null;

				for (Map.Entry<IGitRepo.GitFileStatus, List<String>> o : cmdGit.getFiles(gitCtxt).entrySet())
				{
					fileLoop: for (String file : o.getValue())
					{
						if (vdmOnly && !file.toLowerCase().endsWith(".vdmrt"))
						{
							continue fileLoop;
						}
						for (String exlude : excludePrefix)
						{
							if (exlude != null && file.startsWith(exlude))
							{
								continue fileLoop;
							}
						}

						if (first)
						{
							agentMsg = IntoTraceProtocol.createAgent(gitCtxt, cmdGit);
							res.merge(agentMsg);
							activity = IntoTraceProtocol.createActivity(agentMsg.getCurrentId(), cmdGit.getGitCommitDate(gitCtxt));
							res.merge(activity);
							first = false;
						}

						logger.trace("\tProceeding file: {} - {}", o.getKey(), file);
						res.merge(IntoTraceProtocol.createSourceFile(file, o.getKey() == IGitRepo.GitFileStatus.Added, sp, gitCtxt, cmdGit, activity.getCurrentId()));
					}
				}
			}

			String message = IntoTraceProtocol.makeRootMessage(res).toString(4);

			if (dryRun)
			{
				System.out.println(message);
			} else
			{
				WebClient.post(hostUrl, message);
			}
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}
}
