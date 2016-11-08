package org.overturetool.tracability.driver;

/**
 * Created by kel on 04/11/16.
 */
public class GitCtxt implements IGitRepoContext
{
	public GitCtxt(String commit, UrlScheme.SchemeType scheme)
	{
		this.commit = commit;
		this.scheme = scheme;
	}

	final String commit;
	 UrlScheme.SchemeType scheme;


	@Override public String getCommit()
	{
		return this.commit;
	}

	@Override public IGitRepoContext changeCommit(String commit)
	{
		return new GitCtxt(commit, scheme);
	}

	@Override public UrlScheme.SchemeType getUrlScheme()
	{
		return this.scheme;
	}

	@Override public void setScheme(UrlScheme.SchemeType type)
	{
		this.scheme = type;
	}
}
