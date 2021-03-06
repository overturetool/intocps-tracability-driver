package org.overturetool.tracability.driver;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by kel on 04/11/16.
 */
public interface IGitRepo
{
	class CommitPathPair{
		final String commitId;
		final String path;

		public CommitPathPair(String commitId, String path)
		{
			this.commitId = commitId;
			this.path = path;
		}
	}
	String getCommitMessage(IGitRepoContext ctxt) throws IOException,
			InterruptedException;

	List<String> getDiff(IGitRepoContext gitCtxt, String previousCommitId,
			String file) throws IOException, InterruptedException;

	List<String> showFile(IGitRepoContext gitCtxt, String file)
			throws IOException, InterruptedException;

	enum GitFileStatus
	{
		Added, Deleted, Modified
	}

	String getPath(String path);

	String getUri(IGitRepoContext repoCtxt, String path) throws IOException,
			InterruptedException;

	String getGitCheckSum(IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException;

	List<String> getCommitAuthor(IGitRepoContext ctxt) throws IOException,
			InterruptedException;

	Date getGitCommitDate(IGitRepoContext ctxt) throws IOException,
			InterruptedException, ParseException;

	CommitPathPair getPreviousCommitId(IGitRepoContext repoCtxt, String path)
			throws IOException, InterruptedException;

	List<String> getCommitHistory(String commit) throws IOException,
			InterruptedException;

	Map<GitFileStatus, List<String>> getFiles(IGitRepoContext ctxt)
			throws IOException, InterruptedException;
}
