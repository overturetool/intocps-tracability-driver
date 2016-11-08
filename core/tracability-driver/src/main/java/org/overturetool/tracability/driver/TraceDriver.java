package org.overturetool.tracability.driver;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.util.*;

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

	public TraceDriver(boolean dryRun, UrlScheme.SchemeType urlScheme)
	{
		this.dryRun = dryRun;
		this.urlScheme = urlScheme;
	}

	public void fullSync(File repoUri, String commitId,
			UrlScheme.SchemeType scheme)
			throws IOException, GitAPIException, JSONException
	{
		final IGitRepo cmdGit = new CmdGitRepo(repoUri);
		try
		{
			final IStructureProvider sp = new IStructureProvider()
			{
				@Override public List<JSONObject> getChildren(
						IGitRepoContext repoCtxt, String parent)
						throws JSONException
				{
					List<JSONObject> list = new Vector<>();
//					try
//					{
//
//						String previoudCommit = cmdGit.getPreviousCommitId(repoCtxt, parent);
//
//						IGitRepoContext ctxt = repoCtxt.changeCommit(previoudCommit);
//						for (Map.Entry<IGitRepo.GitFileStatus, List<String>> o : cmdGit.getFiles(ctxt).entrySet())
//						{
//							for (String file : o.getValue())
//							{
//								if (file.endsWith(parent))
//								{
//									list.addAll(IntoTraceProtocol.createSourceFile(file,
//											o.getKey()
//													== IGitRepo.GitFileStatus.Added, this, ctxt, cmdGit));
//								}
//							}
//						}
//					} catch (IOException e)
//					{
//						e.printStackTrace();
//					} catch (InterruptedException e)
//					{
//						e.printStackTrace();
//					}
					return list;
				}
			};
			List<String> refs = cmdGit.getCommitHistory(commitId);

			IGitRepoContext gitCtxt = new GitCtxt(commitId, scheme);

			List<JSONObject> messages = new Vector<>();

			for (String ref : refs)
			{
				gitCtxt = gitCtxt.changeCommit(ref);

				logger.trace("Proceeding commit: {}",gitCtxt.getCommit());

				for (Map.Entry<IGitRepo.GitFileStatus, List<String>> o : cmdGit.getFiles(gitCtxt).entrySet())
				{
					for (String file : o.getValue())
					{
						logger.trace("\tProceeding file: {} - {}",o.getKey(),file);
						messages.addAll(IntoTraceProtocol.createSourceFile(file,
								o.getKey()
										== IGitRepo.GitFileStatus.Added, sp, gitCtxt, cmdGit));
					}
				}

			}

			String message = IntoTraceProtocol.makeRootMessage(new ArrayList<>(), messages.toArray(new JSONObject[] {})).toString(4);

			if (dryRun)
				System.out.printf(message);
			else
				WebClient.post("http://127.0.0.1:8080/traces/push/json", message);
			int i = 0;
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}
}
