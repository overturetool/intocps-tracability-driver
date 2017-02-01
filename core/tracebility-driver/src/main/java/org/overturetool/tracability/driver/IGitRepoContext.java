package org.overturetool.tracability.driver;

/**
 * Created by kel on 04/11/16.
 */
public interface IGitRepoContext
{
	String getCommit();

	IGitRepoContext changeCommit(String commit);

	UrlScheme.SchemeType getUrlScheme();

	void setScheme(UrlScheme.SchemeType type);
}