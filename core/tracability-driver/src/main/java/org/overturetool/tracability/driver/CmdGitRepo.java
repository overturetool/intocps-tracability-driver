package org.overturetool.tracability.driver;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by kel on 04/11/16.
 */
public class CmdGitRepo implements IGitRepo
{

	final static Logger logger = LoggerFactory.getLogger(CmdGitRepo.class);

	public CmdGitRepo(File repoPath)
	{
		this.repoPath = repoPath;
	}

	final File repoPath;

	@Override
	public String getCommitMessage(IGitRepoContext ctxt) throws IOException,
			InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "log", "-1", "--pretty=%B", ctxt.getCommit());

		return refs.get(0).trim();
	}

	@Override
	public List<String> getDiff(IGitRepoContext ctxt, String previousCommitId,
			String file) throws IOException, InterruptedException
	{
		if (previousCommitId == null)
		{
			return null;
		}

		return CmdCall.call(repoPath, "git", "diff", previousCommitId, ctxt.getCommit(), file);
	}

	@Override
	public List<String> showFile(IGitRepoContext ctxt, String file)
			throws IOException, InterruptedException
	{
		return CmdCall.call(repoPath, "git", "show", ctxt.getCommit() + ":"
				+ file);
	}

	@Override
	public String getPath(String path)
	{
		return null;
	}

	String remote = null;

	void fetchRemote() throws IOException, InterruptedException
	{
		if (remote == null)
		{
			List<String> refs = CmdCall.call(repoPath, "git", "config", "--get", "remote.origin.url");
			if (refs != null)
			{
				remote = refs.get(0);
			}

		}
	}

	@Override
	public String getUri(IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException

	{
		switch (repoCtxt.getUrlScheme())
		{
			case github:
				fetchRemote();
				// final String rawGithubUrl = "https://github.com/%s/%s/blob/%s/%s";
				final String rawGithubUrl = "https://raw.githubusercontent.com/%s/%s/%s/%s";
				// https://raw.githubusercontent.com/into-cps/case-study_single_watertank/1845462e5842b9f9ee01d528599c6be178e84cad/.project.json
				// https://github.com/overturetool/vdm2c/blob/18c8de3d9302410a9f152bca05b8ea553ef6e890/c/pom.xml
				if (remote == null || !remote.contains("github.com:"))
				{
					logger.error("The github scheme: '{}' cannot not be used with remote: {}", repoCtxt.getUrlScheme(), remote);
					repoCtxt.setScheme(UrlScheme.SchemeType.custom);
					logger.warn("Chanding configured scheme to: {}", repoCtxt.getUrlScheme());
					return getUri(repoCtxt, path);
				}

				String user = remote.substring(remote.indexOf("github.com:") + 11);
				String repo = user.substring(user.indexOf("/") + 1);

				user = user.substring(0, user.indexOf("/"));
				repo = repo.substring(0, repo.indexOf(".git"));

				String encodedPath = "";
				for (String s : path.split("/"))
				{
					encodedPath += "/" + s.replace(" ", "%20");// +URLEncoder.encode(s,"utf8");
				}

				if (encodedPath.length() > 0)
				{
					encodedPath = encodedPath.substring(1);
				}

				return String.format(rawGithubUrl, user, repo, repoCtxt.getCommit(), encodedPath);
			case gitlab:
				fetchRemote();
				break;
			case intocps:
				break;
			case custom:
				return "file:"
						+ repoPath.getAbsolutePath().replace(File.separatorChar, '/')
						+ "/" + repoCtxt.getCommit() + "/"
						+ path.replace(File.separatorChar, '/');
		}
		return path;
	}

	@Override
	public String getGitCheckSum(IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "--no-pager", "ls-tree", "-r", repoCtxt.getCommit());

		for (String output : refs)
		{

			String[] o = output.split(" ");

			// 100644 7062a128f556df59142590ef9eb2a72f009ea379 0 readme.md
			String[] hashFile = o[2].split("\t");
			if (hashFile[1].endsWith(path))
			{
				return hashFile[0];
			}

		}

		return "0";
	}

	public List<String> getCommitAuthor(IGitRepoContext ctxt)
			throws IOException, InterruptedException
	{

		List<String> strings = new Vector<>();
		strings.add(CmdCall.call(repoPath, "git", "--no-pager", "log", "-1", "--pretty=format:%an", ctxt.getCommit()).get(0));
		strings.add(CmdCall.call(repoPath, "git", "--no-pager", "log", "-1", "--pretty=format:%ae", ctxt.getCommit()).get(0));

		return strings;
	}

	@Override
	public Date getGitCommitDate(IGitRepoContext ctxt) throws IOException,
			InterruptedException, ParseException
	{
		List<String> strings = CmdCall.call(repoPath, "git", "--no-pager", "log", "-n", "1", "--pretty=format:%cd", "--date=format:%Y-%m-%d %H:%M:%S", ctxt.getCommit());

		DateFormat df = new SimpleDateFormat("yyyy-dd-MM HH:mm:ss");
		Date date = df.parse(strings.get(0));

		return date;
	}

	@Override
	public String getPreviousCommitId(IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "rev-list", repoCtxt.getCommit(), "--", path);
		if (refs.size() > 1)
		{
			return refs.get(1);
		} else
		{
			return null;
		}

	}

	@Override
	public List<String> getCommitHistory(String commit) throws IOException,
			InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "rev-list", commit);

		// Collections.reverse(refs);
		return refs;
	}

	@Override
	public Map<GitFileStatus, List<String>> getFiles(IGitRepoContext ctxt)
			throws IOException, InterruptedException
	{
		Map<GitFileStatus, List<String>> files = new HashMap<>();
		files.put(GitFileStatus.Added, new Vector<>());
		files.put(GitFileStatus.Deleted, new Vector<>());
		files.put(GitFileStatus.Modified, new Vector<>());

		List<String> changes = CmdCall.call(repoPath, "git", "show", "--pretty=format:%H", "--name-status", ctxt.getCommit());

		for (String change : changes)
		{
			if (change.matches("^(A|M|D).*"))
			{
				String name = change.substring(change.indexOf('\t') + 1);

				GitFileStatus type = null;
				if (change.matches("^A.*"))
				{
					type = GitFileStatus.Added;
				} else if (change.matches("^D.*"))
				{
					type = GitFileStatus.Deleted;
				} else if (change.matches("^M.*"))
				{
					type = GitFileStatus.Modified;
				}

				if (type != null)
				{
					files.get(type).add(name);
				}
			}
		}
		return files;
	}
}
