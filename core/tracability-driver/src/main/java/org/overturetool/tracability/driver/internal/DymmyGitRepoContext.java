package org.overturetool.tracability.driver.internal;

import org.overturetool.tracability.driver.IGitRepoContext;
import org.overturetool.tracability.driver.UrlScheme;

/**
 * Created by kel on 04/11/16.
 */
public class DymmyGitRepoContext implements IGitRepoContext
{
	@Override
	public String getCommit()
	{
		return "1234";
	}

	@Override
	public IGitRepoContext changeCommit(String commit)
	{
		return null;
	}

	@Override
	public UrlScheme.SchemeType getUrlScheme()
	{
		return null;
	}

	@Override
	public void setScheme(UrlScheme.SchemeType type)
	{

	}
}
