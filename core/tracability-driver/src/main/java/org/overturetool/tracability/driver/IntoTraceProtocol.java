package org.overturetool.tracability.driver;

/**
 * Created by kel on 03/11/16.
 */

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IntoTraceProtocol
{
	final static Logger logger = LoggerFactory.getLogger(IntoTraceProtocol.class);
	public final static String rdf_about = "rdf:about";
	public final static String SOFTWARETOOL = "softwareTool";
	public final static String ACTIVITY_MODELLING = "modelling";
	public static final String ACTIVITY_MODEL_DESCRIPTION_IMPORT = "modelDescriptionImport";
	public static final String ACTIVITY_FMU_EXPORT = "fmu_export";

	public static class ITMessage
	{
		final Map<Prov, List<JSONObject>> data = new HashMap<>();
		final JSONObject current;
		final Prov currentType;

		public ITMessage(Prov currentType, JSONObject current)
		{
			this.current = current;
			this.currentType = currentType;
			this.add(currentType, current);
		}

		public ITMessage()
		{
			this.current = null;
			this.currentType = null;
		}

		public JSONObject getCurrent()
		{
			return this.current;
		}

		public String getCurrentId() throws JSONException
		{
			return this.current.get(rdf_about).toString();
		}

		public void add(Prov type, List<JSONObject> obj)
		{
			List<JSONObject> list = null;
			if (data.containsKey(type))
			{
				list = data.get(type);
			} else
			{
				list = new Vector<>();
			}

			list.addAll(obj);
			data.put(type, list);
		}

		public void add(Prov type, JSONObject obj)
		{
			this.add(type, Arrays.asList(obj));
		}

		public ITMessage merge(ITMessage other)
		{
			for (Map.Entry<Prov, List<JSONObject>> oe : other.data.entrySet())
			{
				add(oe.getKey(), oe.getValue());
			}
			return this;
		}
	}

	public enum Prov
	{
		Entity("prov:Entity"),

		Agent("prov:Agent"),

		Activity("prov:Activity"),

		WasDerivedFrom("prov:wasDerivedFrom"),

		HasMember("prov:hasMember"),

		WasAssociatedWith("prov:wasAssociatedWith"),

		WasAttributedTo("prov:wasAttributedTo"),

		WasGeneratedBy("prov:wasGeneratedBy"),

		Used("prov:used");

		public final String name;

		Prov(String name)
		{
			this.name = name;
		}
	}

	public enum IntoCps
	{
		Name("name"),

		Email("email"),

		Version("version"),

		Type("type"),

		Hash("hash"),

		Path("path"),

		Commit("commit"),

		Comment("comment"),

		Time("time"),

		Url("url");

		public final String name;

		IntoCps(String name)
		{
			this.name = /*"intocps:" +*/ name;
		}
	}

	public enum IntoCpsTypes
	{
		SoftwareTool("softwareTool"),

		Activity("activity"),

		Source("source"),

		Fmu("fmu"),

		ModelDescription("modelDescription");

		public final String name;

		IntoCpsTypes(String name)
		{
			this.name = "intocps:" + name;
		}
	}

	public static <T> Predicate<T> distinctByKey(
			Function<? super T, Object> keyExtractor)
	{
		Map<Object, String> seen = new ConcurrentHashMap<>();
		return t -> seen.put(keyExtractor.apply(t), "") == null;
	}

	public static JSONObject makeRootMessage(ITMessage msg) throws JSONException
	{
		JSONObject body = new JSONObject();

		body.put("xmlns:rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		body.put("xmlns:prov", "http://www.w3.org/ns/prov#");
		body.put("xmlns:intocps", "http://www.w3.org/ns/intocps#");
		body.put("messageFormatVersion", "0.2");

		for (Map.Entry<Prov, List<JSONObject>> entry : msg.data.entrySet())
		{
			List<JSONObject> list = entry.getValue();
			if (list != null && !list.isEmpty())
			{

				List<JSONObject> filteredList = list.stream().filter(distinctByKey(p ->
				{
					try
					{
						return p.get(rdf_about);
					} catch (JSONException e)
					{
						e.printStackTrace();
						return null;
					}
				})).collect(Collectors.toList());

				body.put(entry.getKey().name, new JSONArray(filteredList));
			}
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

	public static ITMessage createAgent(IGitRepoContext repoCtxt, IGitRepo repo)
			throws IOException, InterruptedException, JSONException
	{
		List<String> author = repo.getCommitAuthor(repoCtxt);
		ITMessage authorObject = createAgent(author.get(0), author.get(1));
		return authorObject;
	}

	public static ITMessage createAgent(String name, String email)
			throws JSONException
	{
		JSONObject obj = new JSONObject();

		obj.put(rdf_about, getId(Prov.Agent, name.replace(' ', '_')));
		obj.put(IntoCps.Name.name, name);
		obj.put(IntoCps.Email.name, email);

		return new ITMessage(Prov.Agent, obj);
	}

	public static ITMessage createTool(String name, String version)
			throws JSONException
	{
		JSONObject obj = new JSONObject();

		obj.put(rdf_about, getId(Prov.Entity, SOFTWARETOOL, name, version));
		obj.put(IntoCps.Version.name, version);
		obj.put(IntoCps.Type.name, IntoCpsTypes.SoftwareTool.name);
		obj.put(IntoCps.Name.name, name);

		return new ITMessage(Prov.Entity, obj);
	}

	public static ITMessage createActivity(String agentId, String activityName,
			Date date, ITMessage toolMsg) throws JSONException
	{
		JSONObject obj = new JSONObject();

		TimeZone tz = TimeZone.getTimeZone("UTC");
		DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX"); // Quoted "Z" to indicate UTC, no timezone offset
		formatter.setTimeZone(tz);

		String time = formatter.format(date);
		obj.put(rdf_about, getId(Prov.Activity, activityName, time));
		obj.put(IntoCps.Time.name, time);
		obj.put(IntoCps.Type.name, IntoCpsTypes.Activity.name);
		if (agentId != null)
		{
			obj.put(Prov.WasAssociatedWith.name, mkObject(Prov.Agent.name, mkObject(rdf_about, agentId)));
		}

		obj.put(Prov.Used.name, mkObject(Prov.Entity.name, mkObject(rdf_about, toolMsg.getCurrentId())));

		ITMessage msg = new ITMessage(Prov.Activity, obj);
		msg.merge(toolMsg);
		msg.add(Prov.Activity, obj);
		return msg;
	}

	public static String getId(Prov type, String... args)
	{
		switch (type)
		{
			case Entity:

				if (args.length > 0 && SOFTWARETOOL.equals(args[0]))
				{
					return String.format("Entity.%s:%s:%s", SOFTWARETOOL, args[1], args[2]);
				}

				if (args.length == 2)
				{
					// Entity.<entity type>:<git relative path>#<githash of the document>
					return String.format("Entity.%s:%s#%s", "source", args[0], args[1]);
				} else if (args.length == 3)
				{
					// Entity.<entity type>:<git relative path>:<subpart name>#<githash of the document>
					return String.format("Entity.%s:%s:%s#%s", "source", args[0], args[1], args[2]);
				}
			case Agent:
				// Agent:<unique username>
				return String.format("Agent:%s", args[0]);
			case Activity:
				// Activity.<activity type>:<time in format yyyy-mm-dd-hh-mm-ss>#
				return String.format("Activity.%s:%s:%s", args[0], args[1], UUID.randomUUID());
			case WasDerivedFrom:
				break;
			case HasMember:
				break;
			case WasAssociatedWith:
				break;
			case WasAttributedTo:
				break;
			case WasGeneratedBy:
				break;
			case Used:
				break;
		}
		return null;
	}

	/***
	 * get a list of file entries where the first entry is the requested root file
	 *
	 * @param structureProvider
	 * @param repo
	 * @return
	 */
	public static ITMessage createSourceFile(String path, boolean added,
			IStructureProvider structureProvider, IGitRepoContext repoCtxt,
			IGitRepo repo, String activityId)
			throws IOException, InterruptedException, JSONException
	{

		JSONObject obj = new JSONObject();
		String uri = repo.getUri(repoCtxt, path);
		logger.trace("Creating entry for: {}", uri);
		logger.trace("\tCommit: {} Path: {}", repoCtxt.getCommit(),path);

		obj.put(rdf_about, getId(Prov.Entity, path, repo.getGitCheckSum(repoCtxt, path)));
		obj.put(IntoCps.Url.name, uri);
		obj.put(IntoCps.Path.name, path);
		obj.put(IntoCps.Hash.name, repo.getGitCheckSum(repoCtxt, path));
		obj.put(IntoCps.Commit.name, repoCtxt.getCommit());
		obj.put(IntoCps.Comment.name, repo.getCommitMessage(repoCtxt));
		obj.put(IntoCps.Type.name, IntoCpsTypes.Source.name);

		ITMessage map = new ITMessage(Prov.Entity, obj);
		ITMessage authorObject = createAgent(repoCtxt, repo);
		map.merge(authorObject);
		obj.put(Prov.WasAttributedTo.name, mkObject(Prov.Agent.name, mkObject(rdf_about, authorObject.getCurrentId())));

		if (activityId != null)
		{
			obj.put(Prov.WasGeneratedBy.name, mkObject(Prov.Activity.name, mkObject(rdf_about, activityId)));
		}

		if (!added)
		{
			JSONArray derivedList = new JSONArray();

			IGitRepo.CommitPathPair previousCommit = repo.getPreviousCommitId(repoCtxt, path);
			//String priviousUrl = repo.getUri(repoCtxt.changeCommit(previousCommit.commitId), previousCommit.path);// TODO
			// adjust
			// to
			// previous
			// commit

			logger.trace("\tDetected previous revision at: {}", previousCommit.commitId);
			logger.trace("\t\t\t\tFrom commit: {} and path: {}", previousCommit.commitId,previousCommit.path);

			JSONObject pObj = new JSONObject();
			pObj.put(rdf_about, getId(Prov.Entity, previousCommit.path, repo.getGitCheckSum(repoCtxt.changeCommit(previousCommit.commitId), previousCommit.path)));

			derivedList.put(mkObject(Prov.Entity.name, pObj));

			obj.put(Prov.WasDerivedFrom.name, derivedList);
		}

		List<JSONObject> children = structureProvider.getChildren(repoCtxt, path, obj);

		List<JSONObject> list = new Vector<>();
		if (children != null)
		{
			for (JSONObject child : children)
			{
				JSONObject m = new JSONObject();
				m.put(rdf_about, child.get(rdf_about));
				list.add(mkObject(Prov.Entity.name, m));
			}

			obj.put(Prov.HasMember.name, new JSONArray(list));
		}

		// list.add(0, obj);

		// map.add(Prov.Entity, obj);
		map.add(Prov.Entity, children);
		// map.add(Prov.Agent, authorObject);

		return map;
	}

	public static ITMessage createBasicEntity(String name, String hash)
			throws JSONException
	{
		JSONObject obj = new JSONObject();
		obj.put(rdf_about, getId(Prov.Entity, name, hash));
		obj.put(IntoCps.Hash.name, hash);
		obj.put(IntoCps.Type.name, IntoCpsTypes.Source.name);

		return new ITMessage(Prov.Entity, obj);
	}
}
