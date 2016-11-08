package org.overturetool.tracability.driver;

/**
 * Created by kel on 03/11/16.
 */

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

public class IntoTraceProtocol
{
	final static Logger logger = LoggerFactory.getLogger(IntoTraceProtocol.class);
	public final static String rdf_about = "rdf:about";

	public static JSONObject makeRootMessage(List<JSONObject> agents,
			JSONObject... entities) throws JSONException
	{
		JSONObject body = new JSONObject();

		body.put("xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		body.put("xmlns:prov", "http://www.w3.org/ns/prov#");
		body.put("messageFormatVersion", "0.1");

		if (entities.length > 0)
		{
			body.put("prov:Entity", new JSONArray(Arrays.asList(entities)));
		}

		if (!agents.isEmpty())
		{
			body.put("prov:Agent", new JSONArray(agents));
		}

		JSONObject root = new JSONObject();
		root.put("rdf:RDF", body);

		return root;
	}

	static JSONObject mkObject(String key, Object obj) throws JSONException
	{
		JSONObject root = new JSONObject();
		root.put(key, obj);
		return root;
	}

	public static JSONObject createAgent(String name, String email)
			throws JSONException
	{
		JSONObject obj = new JSONObject();

		obj.put(rdf_about, String.format("Agent:%s", name));
		obj.put("email", email);

		return obj;
	}

	public static JSONObject createTool(String name, String version)
			throws JSONException
	{
		JSONObject obj = new JSONObject();

		obj.put(rdf_about, String.format("Entity.softwareTool:%s:%s", name, version));
		obj.put("version", version);
		obj.put("type", "softwareTool");
		obj.put("name", name);

		return obj;
	}

	/***
	 * get a list of file entries where the first entry is the requested root file
	 * @param file
	 * @param structureProvider
	 * @param repo
	 * @return
	 */
	public static List<JSONObject> createSourceFile(String path, boolean added,
			IStructureProvider structureProvider, IGitRepoContext repoCtxt,
			IGitRepo repo)
			throws IOException, InterruptedException, JSONException
	{

		JSONObject obj = new JSONObject();
		String uri = repo.getUri(repoCtxt, path);
		logger.trace("\t\tCreating entry for: {}", uri);
		obj.put(rdf_about, uri);
		obj.put("path", path);
		obj.put("hash", repo.getGitCheckSum(repoCtxt, path)); //TODO what is a hash
		obj.put("comment", repo.getCommitMessage(repoCtxt));

		List<String> author = repo.getCommitAuthor(repoCtxt);

		if (!added)
		{
			JSONArray derivedList = new JSONArray();

			String priviousUrl = repo.getUri(repoCtxt.changeCommit(repo.getPreviousCommitId(repoCtxt, path)), path);//TODO adjust to previous commit

			logger.trace("\t\t\tDetected previous revision at: {}", priviousUrl);

			JSONObject pObj = new JSONObject();
			pObj.put(rdf_about, priviousUrl);

			derivedList.put(mkObject("prov:Entity",pObj));

			obj.put("prov:wasDerivedFrom", derivedList);
		}

		List<JSONObject> children = structureProvider.getChildren(repoCtxt, path);

		List<JSONObject> list = new Vector<>();
		if (children != null)
		{
			for (JSONObject child : children)
			{
				JSONObject m = new JSONObject();
				m.put(rdf_about, child.get(rdf_about));
				list.add(mkObject("prov:Entity", m));
			}

			obj.put("prov.hasMember", new JSONArray(list));
		}

		list.add(0, obj);

		return list;
	}
}
