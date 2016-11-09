package org.overturetool.tracability.driver.internal;

import org.overturetool.tracability.driver.IGitRepo;
import org.overturetool.tracability.driver.IGitRepoContext;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Created by kel on 04/11/16.
 */
public class DummyGitRepo implements IGitRepo
{
	@Override public String getCommitMessage(IGitRepoContext ctxt)
			throws IOException, InterruptedException
	{
		return "";
	}

	@Override public String getPath(String path)
	{
		return "my/path/";
	}

	@Override public String getUri(IGitRepoContext repoCtxt, String path)
	{
		return "git:/uri";
	}

	@Override public String getGitCheckSum(IGitRepoContext repoCtxt,
			String path)
	{
		return "0000";
	}

	@Override public List<String> getCommitAuthor(IGitRepoContext ctxt)
			throws IOException, InterruptedException
	{
		return null;
	}

	@Override public Date getGitCommitDate(IGitRepoContext ctxt)
	{
		return null;
	}

	@Override public String getPreviousCommitId(
			IGitRepoContext repoCtxt, String path)
	{
		return "4321";
	}

	@Override public List<String> getCommitHistory(String commit)
	{
		return new ArrayList<>();
	}

	@Override public Map<GitFileStatus, List<String>> getFiles(IGitRepoContext ctxt)
	{
		return null;
	}
}
