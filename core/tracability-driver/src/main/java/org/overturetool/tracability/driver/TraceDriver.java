package org.overturetool.tracability.driver;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.eclipse.jgit.api.errors.GitAPIException;

import java.io.*;
import java.text.ParseException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sun.org.apache.xml.internal.serializer.utils.Utils.messages;

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

	public TraceDriver(boolean dryRun, UrlScheme.SchemeType urlScheme,
			List<String> excludePathPrefix, boolean vdmOnly)
	{
		this.dryRun = dryRun;
		this.urlScheme = urlScheme;
		this.excludePrefix = excludePathPrefix;
		this.vdmOnly = vdmOnly;
	}

	public void fullSync(File repoUri, String commitId,
			UrlScheme.SchemeType scheme)
			throws IOException, GitAPIException, JSONException, ParseException
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

			//			List<JSONObject> entities = new Vector<>();
			//			List<JSONObject> agents = new Vector<>();
			IntoTraceProtocol.ITMessage res = new IntoTraceProtocol.ITMessage();

			for (String ref : refs)
			{
				gitCtxt = gitCtxt.changeCommit(ref);

				logger.trace("Proceeding commit: {}", gitCtxt.getCommit());

				boolean first= true;

				IntoTraceProtocol.ITMessage agentMsg = null;
				IntoTraceProtocol.ITMessage activity = null;


				for (Map.Entry<IGitRepo.GitFileStatus, List<String>> o : cmdGit.getFiles(gitCtxt).entrySet())
				{
					fileLoop:
					for (String file : o.getValue())
					{
						if(vdmOnly && !file.toLowerCase().endsWith(".vdmrt"))
							continue fileLoop;
						for (String exlude : excludePrefix)
						{
							if (file.startsWith(exlude))
								continue fileLoop;
						}

						if(first)
						{
							 agentMsg = IntoTraceProtocol.createAgent(gitCtxt, cmdGit);
							res.merge(agentMsg);
							 activity = IntoTraceProtocol.createActivity(agentMsg.getCurrentId(), cmdGit.getGitCommitDate(gitCtxt));
							res.merge(activity);
							first = false;
						}


						logger.trace("\tProceeding file: {} - {}", o.getKey(), file);
						res.merge(IntoTraceProtocol.createSourceFile(file,
								o.getKey()
										== IGitRepo.GitFileStatus.Added, sp, gitCtxt, cmdGit, activity.getCurrentId()));

						//						if (res.containsKey(IntoTraceProtocol.Prov.Entity))
						//						{
						//							entities.addAll(res.get(IntoTraceProtocol.Prov.Entity));
						//						}
						//						if (res.containsKey(IntoTraceProtocol.Prov.Agent))
						//						{
						//							agents.addAll(res.get(IntoTraceProtocol.Prov.Agent));
						//						}

					}
				}

			}

			String message = IntoTraceProtocol.makeRootMessage(res).toString(4);

			if (dryRun)
			{
				System.out.println(message);
			} else
				WebClient.post("http://127.0.0.1:8080/traces/push/json", message);
			int i = 0;
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}
}
