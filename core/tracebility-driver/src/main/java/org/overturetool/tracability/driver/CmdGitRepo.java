package org.overturetool.tracability.driver;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

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

	@Override public String getCommitMessage(IGitRepoContext ctxt)
			throws IOException, InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "log", "-1", "--pretty=%B", ctxt.getCommit());

		return refs.get(0).trim();
	}

	@Override public List<String> getDiff(IGitRepoContext ctxt,
			String previousCommitId, String file)
			throws IOException, InterruptedException
	{
		if (previousCommitId == null)
		{
			return null;
		}

		List<String> tmp = CmdCall.call(false, repoPath, "git", "diff", "-C", previousCommitId, ctxt.getCommit(), file);

		if (tmp == null)
		{

			//try to find the previous used name
			List<String> oldNames = CmdCall.call(repoPath, "git", "log", "--follow", "--pretty=%H", "--name-status", ctxt.getCommit(), "--", file);

			Iterator<String> itr = oldNames.iterator();
			while (itr.hasNext())
			{
				String line = itr.next();
				if (line.trim().equals(previousCommitId))
				{
					itr.next();
					String mod = itr.next();

					String renamedFrom = mod.split("\t")[1];

					tmp = CmdCall.call(repoPath, "git", "diff", "-C", previousCommitId, ctxt.getCommit(), "--", renamedFrom, file);
					return tmp;

				}
			}

			//git log --follow --pretty=%H --name-status su\ b/HardwareInterface.vdmrt
			//git diff -C f384c10a845ad616c80f5f9354378e4874dd662d 23e229210dd5b70a1671963ef4a7b520d7f69fcb -- HardwareInterface.vdmrt sub/HardwareInterface.vdmrt
		}
		return tmp;
	}

	@Override public List<String> showFile(IGitRepoContext ctxt, String file)
			throws IOException, InterruptedException
	{
		return CmdCall.call(repoPath, "git", "show",
				ctxt.getCommit() + ":" + file);
	}

	@Override public String getPath(String path)
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

	@Override public String getUri(IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException

	{
		return getInternalUri(repoCtxt, path).replace(" ", "%20");
	}

	String getInternalUri(IGitRepoContext repoCtxt, String path)
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
				if (remote == null || !remote.contains("github.com"))
				{
					logger.error("The github scheme: '{}' cannot not be used with remote: {}", repoCtxt.getUrlScheme(), remote);
					repoCtxt.setScheme(UrlScheme.SchemeType.custom);
					logger.warn("Chanding configured scheme to: {}", repoCtxt.getUrlScheme());
					return getUri(repoCtxt, path);
				}

				String user;

				if (remote.contains("github.com:"))
				{
					user = remote.substring(remote.indexOf("github.com:") + 11);
				} else
				{
					user = remote.substring(remote.indexOf("github.com/") + 11);
				}

				String repo = user.substring(user.indexOf("/") + 1);

				user = user.substring(0, user.indexOf("/"));
				repo = repo.substring(0, repo.indexOf(".git"));

				String encodedPath = "";
				for (String s : path.split("/"))
				{
					encodedPath += "/"
							+ s.replace(" ", "%20");// +URLEncoder.encode(s,"utf8");
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

	@Override public String getGitCheckSum(IGitRepoContext repoCtxt,
			String path) throws IOException, InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "--no-pager", "ls-tree", "-r", repoCtxt.getCommit());

		for (String output : refs)
		{
			//<mode> SP <type> SP <object> TAB <file>
			// 100644 blob ce013625030ba8dba906f756967f9e9ca394464a	su b/HardwareInterface.vdmrt
			int index = output.indexOf(' ');
			String mode = output.substring(0, index);
			output = output.substring(index + 1);

			index = output.indexOf(' ');
			String type = output.substring(0, index);
			output = output.substring(index + 1);

			index = output.indexOf('\t');
			String object = output.substring(0, index);
			output = output.substring(index + 1);

			String file = output;

			if (file.endsWith(path))
			{
				return object;
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

	@Override public Date getGitCommitDate(IGitRepoContext ctxt)
			throws IOException, InterruptedException, ParseException
	{
		List<String> strings = CmdCall.call(repoPath, "git", "--no-pager", "log", "-n", "1", "--pretty=format:%cd", "--date=format:%Y-%m-%d %H:%M:%S", ctxt.getCommit());

		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = df.parse(strings.get(0));

		return date;
	}

	@Override public CommitPathPair getPreviousCommitId(
			IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException
	{
		//List<String> refs = CmdCall.call(repoPath, "git", "rev-list", repoCtxt.getCommit(), "--", path);
		List<String> refs = CmdCall.call(repoPath, "git", "log", "--follow", "--name-status", "--pretty=format:H %H", "-n", "2", repoCtxt.getCommit(), "--", path);
		if (refs.size() > 4)
		{
			Iterator<String> itr = refs.iterator();
			//itr.next();//skip first;
			String hash = null;
			String line = null;
			String newPath = null;
			while (itr.hasNext() && (line = itr.next()) != null)
			{
				if (line.startsWith("H "))
				{

					hash = line.substring(2);
					newPath = null;

				} else if (line.startsWith("R100"))
				{
					newPath = line.split("\t")[2];
				} else if (line.startsWith("M") || line.startsWith("A"))
				{
					newPath = line.split("\t")[1];
				}
			}

			return new CommitPathPair(hash, newPath == null ? path : newPath);
		} else
		{
			return null;
		}

	}

	@Override public List<String> getCommitHistory(String commit)
			throws IOException, InterruptedException
	{
		List<String> refs = CmdCall.call(repoPath, "git", "rev-list", commit);

		// Collections.reverse(refs);
		return refs;
	}

	@Override public Map<GitFileStatus, List<String>> getFiles(
			IGitRepoContext ctxt) throws IOException, InterruptedException
	{
		Map<GitFileStatus, List<String>> files = new HashMap<>();
		files.put(GitFileStatus.Added, new Vector<>());
		files.put(GitFileStatus.Deleted, new Vector<>());
		files.put(GitFileStatus.Modified, new Vector<>());

		List<String> changes = CmdCall.call(repoPath, "git", "show", "--pretty=format:%H", "-M", "--name-status", ctxt.getCommit());

		for (String change : changes)
		{
			if (change.matches("^(A|M|D|R100).*"))
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
				} else if (change.matches("^R100.*"))
				{
					type = GitFileStatus.Modified;
					name = change.split("\t")[2];
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
