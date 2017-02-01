package org.overturetool.tracability.driver;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;

import org.apache.sling.commons.json.JSONArray;
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
							IntoTraceProtocol.ITMessage toolMsg = IntoTraceProtocol.createTool("Overture", "2.4.0");
							activity = IntoTraceProtocol.createActivity(agentMsg.getCurrentId(), IntoTraceProtocol.ACTIVITY_MODELLING, cmdGit.getGitCommitDate(gitCtxt), toolMsg);
							res.merge(activity);
							first = false;
						}

						logger.trace("\tProceeding file: {} - {}", o.getKey(), file);
						IntoTraceProtocol.ITMessage sourceFileTrace = IntoTraceProtocol.createSourceFile(file, o.getKey() == IGitRepo.GitFileStatus.Added, sp, gitCtxt, cmdGit, activity.getCurrentId());
						res.merge(sourceFileTrace);

						if (file.endsWith("HardwareInterface.vdmrt"))
						{
							IntoTraceProtocol.ITMessage importExportTrace = generateImportExportTraceInfo(gitCtxt, repoUri, cmdGit, file, sourceFileTrace, agentMsg.getCurrentId());
							if (importExportTrace != null)
							{
								res.merge(importExportTrace);
							}
						}
					}
				}
			}

			String message = IntoTraceProtocol.makeRootMessage(res).toString(4);

			if (dryRun)
			{
				System.out.println(message);
			} else
			{
				WebClient.post(hostUrl + "/traces/push/json", message);
			}
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}

	}

	public IntoTraceProtocol.ITMessage generateImportExportTraceInfo(
			IGitRepoContext gitCtxt, File repoUri, IGitRepo cmdGit,
			String file, IntoTraceProtocol.ITMessage sourceFileTrace,
			String agentId) throws IOException, InterruptedException,
			JSONException, ParseException
	{
		IntoTraceProtocol.ITMessage importExportMsg = null;
		List<String> traces = null;
		IGitRepo.CommitPathPair previousCommit = cmdGit.getPreviousCommitId(gitCtxt, file);
		if (previousCommit != null)
		{
			List<String> diff = cmdGit.getDiff(gitCtxt, previousCommit.commitId, file);
			traces = diff.stream().filter(l -> l.startsWith("+--##\tIMPORT")
					|| l.startsWith("+--##\tEXPORT")).collect(Collectors.toList());
		} else
		{

			List<String> lines = cmdGit.showFile(gitCtxt, file);
			traces = lines.stream().filter(l -> l.startsWith("--##\tIMPORT")
					|| l.startsWith("--##\tEXPORT")).collect(Collectors.toList());

		}
		if (!traces.isEmpty())
		{
			if (importExportMsg == null)
			{
				importExportMsg = new IntoTraceProtocol.ITMessage();
			}
			for (String trace : traces)
			{
				String[] info = trace.split("\t");
				if (info.length >= 6)
				{
					boolean isImport = "IMPORT".equals(info[1]);
					boolean isExport = "EXPORT".equals(info[1]);

					if (isImport || isExport)
					{
						String hash = info[2];
						String fileName = info[3];
						DateFormat df = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
						Date date = df.parse(info[4]);

						String toolInfo = info[6];
						JSONObject t = new JSONObject(toolInfo);
						IntoTraceProtocol.ITMessage toolMsg = IntoTraceProtocol.createTool(t.getString("name"), t.getString("version"));
						importExportMsg.merge(toolMsg);

						if (isImport)
						{
							String mdId = null;

							try
							{
								// http://127.0.0.1:8080/nodes/json?hash=a9d237d2530deea0c71b11a8eff8fb3809165848
								// try to obtain the ID first
								String reply = WebClient.get(hostUrl
										+ "/nodes/json?hash=" + hash);

								JSONArray arr = new JSONArray(reply);
								if (arr.length() > 0)
								{
									mdId = ((JSONObject) ((JSONObject) ((JSONObject) arr.get(1)).get("node")).get("properties")).get("uri").toString();
								}
							} catch (Exception e)
							{
								logger.warn("Could not obtain id from database", e);
							}

							if (mdId == null)
							{
								IntoTraceProtocol.ITMessage msg = IntoTraceProtocol.createBasicEntity(fileName, hash);
								msg.getCurrent().put(IntoTraceProtocol.IntoCps.Type.name, IntoTraceProtocol.IntoCpsTypes.ModelDescription.name);
								importExportMsg.merge(msg);
								mdId = msg.getCurrentId();
							}

							IntoTraceProtocol.ITMessage activity = IntoTraceProtocol.createActivity(agentId, IntoTraceProtocol.ACTIVITY_MODEL_DESCRIPTION_IMPORT, date, toolMsg);
							activity.getCurrent().put(IntoTraceProtocol.Prov.Used.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.Prov.Entity.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.rdf_about, mdId)));

							importExportMsg.merge(activity);
							sourceFileTrace.getCurrent().put(IntoTraceProtocol.Prov.WasGeneratedBy.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.Prov.Activity.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.rdf_about, activity.getCurrentId())));
						} else if (isExport)
						{
							IntoTraceProtocol.ITMessage msg = IntoTraceProtocol.createBasicEntity(fileName, hash);
							msg.getCurrent().put(IntoTraceProtocol.IntoCps.Type.name, IntoTraceProtocol.IntoCpsTypes.Fmu.name);
							importExportMsg.merge(msg);

							IntoTraceProtocol.ITMessage activity = IntoTraceProtocol.createActivity(agentId, IntoTraceProtocol.ACTIVITY_FMU_EXPORT, date, toolMsg);
							importExportMsg.merge(activity);
							msg.getCurrent().put(IntoTraceProtocol.Prov.WasGeneratedBy.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.Prov.Activity.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.rdf_about, activity.getCurrentId())));
							msg.getCurrent().put(IntoTraceProtocol.Prov.Used.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.Prov.Entity.name, IntoTraceProtocol.mkObject(IntoTraceProtocol.rdf_about, sourceFileTrace.getCurrentId())));

						}
					}
				}
			}
			System.out.println();
		}
		return importExportMsg;
	}
}
