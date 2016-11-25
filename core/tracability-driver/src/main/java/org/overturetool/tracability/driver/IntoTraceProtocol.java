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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class IntoTraceProtocol
{
	final static Logger logger = LoggerFactory.getLogger(IntoTraceProtocol.class);
	public final static String rdf_about = "rdf:about";
	public final static String url = "url";
	public final static String SOFTWARETOOL = "softwareTool";

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
				list = data.get(type);
			else
				list = new Vector<>();

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
		body.put("messageFormatVersion", "0.1");

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

		//		if (entities != null && entities.size() > 0)
		//		{
		//			//body.put(Prov.Entity.name, new JSONArray(Arrays.asList(entities)));
		//			body.put(Prov.Entity.name, new JSONArray(entities.stream().filter(distinctByKey(p ->
		//			{
		//				try
		//				{
		//					return p.get(rdf_about);
		//				} catch (JSONException e)
		//				{
		//					e.printStackTrace();
		//					return null;
		//				}
		//			})).collect(Collectors.toList())));
		//		}
		//
		//		if (agents != null && !agents.isEmpty())
		//		{
		//			body.put(Prov.Agent.name, new JSONArray(agents.stream().filter(distinctByKey(p ->
		//			{
		//				try
		//				{
		//					return p.get(rdf_about);
		//				} catch (JSONException e)
		//				{
		//					e.printStackTrace();
		//					return null;
		//				}
		//			})).collect(Collectors.toList())));
		//		}

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
		obj.put("name", name);
		obj.put("email", email);

		return new ITMessage(Prov.Agent, obj);
	}

	public static ITMessage createTool(String name, String version)
			throws JSONException
	{
		JSONObject obj = new JSONObject();

		obj.put(rdf_about,getId(Prov.Entity,SOFTWARETOOL, name, version));
		obj.put("version", version);
		obj.put("type", "softwareTool");
		obj.put("name", name);

		return new ITMessage(Prov.Entity, obj);
	}

	public static ITMessage createActivity(String agentId, Date date)
			throws JSONException
	{
		JSONObject obj = new JSONObject();

		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");

		String time = formatter.format(date);
		obj.put(rdf_about,getId(Prov.Activity,"modelling", time));
		obj.put("time", time);
		obj.put("type", "activity");
		if (agentId != null)
		{
			obj.put(Prov.WasAssociatedWith.name, mkObject(Prov.Agent.name, mkObject(rdf_about, agentId)));
		}
		ITMessage toolMsg = createTool("Overture", "2.4.0");
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
					//Entity.<entity type>:<git relative path>#<githash of the document>
					return String.format("Entity.%s:%s#%s", "source", args[0], args[1]);
				else if (args.length == 3)
					//Entity.<entity type>:<git relative path>:<subpart name>#<githash of the document>
					return String.format("Entity.%s:%s:%s#%s", "source", args[0], args[1], args[2]);
			case Agent:
				//Agent:<unique username>
				return String.format("Agent:%s",args[0]);
			case Activity:
				//Activity.<activity type>:<time in format yyyy-mm-dd-hh-mm-ss>#
				return String.format("Activity.%s:%s:%s", args[0],args[1],UUID.randomUUID());
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
	 * @param structureProvider
	 * @param repo
	 * @return
	 */
	public static ITMessage createSourceFile(String path, boolean added,
			IStructureProvider structureProvider, IGitRepoContext repoCtxt,
			IGitRepo repo, String activityId)
			throws IOException, InterruptedException, JSONException
	{
		ITMessage map = new ITMessage();

		JSONObject obj = new JSONObject();
		String uri = repo.getUri(repoCtxt, path);
		logger.trace("\t\tCreating entry for: {}", uri);
		obj.put(rdf_about, getId(Prov.Entity,path,repo.getGitCheckSum(repoCtxt,path)));
		obj.put(url, uri);
		obj.put("path", path);
		obj.put("hash", repo.getGitCheckSum(repoCtxt, path)); //TODO what is a hash
		obj.put("comment", repo.getCommitMessage(repoCtxt));
		obj.put("type", "source");

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

			String priviousUrl = repo.getUri(repoCtxt.changeCommit(repo.getPreviousCommitId(repoCtxt, path)), path);//TODO adjust to previous commit

			logger.trace("\t\t\tDetected previous revision at: {}", priviousUrl);

			JSONObject pObj = new JSONObject();
			pObj.put(rdf_about, priviousUrl);

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

		//list.add(0, obj);

		map.add(Prov.Entity, obj);
		map.add(Prov.Entity, children);
		//map.add(Prov.Agent, authorObject);

		return map;
	}
}
