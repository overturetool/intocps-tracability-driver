package org.overturetool.tracability.driver;

import java.util.List;

import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

/**
 * Created by kel on 04/11/16.
 */
public interface IStructureProvider
{
	List<JSONObject> getChildren(IGitRepoContext repoCtxt, String parent,
			JSONObject obj) throws JSONException;
}
