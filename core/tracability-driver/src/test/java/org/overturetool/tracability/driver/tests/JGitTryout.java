package org.overturetool.tracability.driver.tests;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.TreeWalk;

import com.google.common.collect.Lists;

/**
 * Created by kel on 04/11/16.
 */
public class JGitTryout
{

	public void test() throws IOException, GitAPIException
	{
		File repoUri = new File(".git");
		String commitId = "HEAD";

		Repository repo = new FileRepositoryBuilder().setGitDir(repoUri).build();
		Git git = null;

		try
		{
			git = new Git(repo);

			// ObjectId list = git.g.resolve(String.format("git rev-list %s", commit);
			// git.exactRef()

			{

				Iterable<RevCommit> commits = git.log().add(repo.resolve(commitId)).call();

				try (ObjectReader reader = repo.newObjectReader())
				{
					CanonicalTreeParser treeParser = new CanonicalTreeParser(null, reader, commits.iterator().next().getTree());
					// boolean haveFile = treeParser.findFile(testFileName);
					// System.out.println("test file in commit", haveFile);
					System.out.println(treeParser.getEntryPathString());
					ObjectId objectForInitialVersionOfFile = treeParser.getEntryObjectId();

					// now we have the object id of the file in the commit:
					// open and read it from the reader
					ObjectLoader oLoader = reader.open(objectForInitialVersionOfFile);
					ByteArrayOutputStream contentToBytes = new ByteArrayOutputStream();
					oLoader.copyTo(contentToBytes);
					// System.out.println("initial content", new String(contentToBytes.toByteArray(), "utf-8"));
				}
			}

			ObjectId commitRef = repo.resolve(commitId);

			{

				RevWalk walk = new RevWalk(repo);

				RevCommit commit = walk.parseCommit(commitRef);
				RevTree tree = commit.getTree();
				System.out.println("Having tree: " + tree);

				// now use a TreeWalk to iterate over all files in the Tree recursively
				// you can set Filters to narrow down the results if needed
				TreeWalk treeWalk = new TreeWalk(repo);
				treeWalk.addTree(tree);
				treeWalk.setRecursive(true);
				while (treeWalk.next())
				{

					System.out.println("found: " + treeWalk.getPathString());
				}

			}

			// ReflogReader refLog = repo.getReflogReader(commitRef);
			//
			// for(ReflogEntry r : refLog.getReverseEntries())
			// {
			// System.out.printf( r.getComment());
			// }

			List<Ref> branches = git.branchList().call();

			for (Ref branch : branches)
			{
				String branchName = branch.getName();

				System.out.println("Commits of branch: " + branchName);
				System.out.println("-------------------------------------");

				Iterable<RevCommit> commits = git.log().add(repo.resolve(branchName)).call();

				List<RevCommit> commitsList = Lists.newArrayList(commits.iterator());

				for (RevCommit commit : commitsList)
				{
					System.out.println(commit.getName());
					System.out.println(commit.getAuthorIdent().getName());
					System.out.println(new Date(commit.getCommitTime() * 1000L));
					System.out.println(commit.getFullMessage());
				}
			}
		} finally
		{
			if (git != null)
			{
				git.close();
			}
		}
	}
}
