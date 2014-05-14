package org.jenkinsci.plugins.tokenplugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.junit.Assert;
import org.junit.Test;

public class GetTokenTest {

	@Test
	public void testTreeMapSortBehaviour() throws Exception {
		HashMap<String, String> map = new HashMap<String, String>();
		map.put("A", "2");
		map.put("C", "3");
		map.put("B", "1");
		map.put("D", "4");

		System.out.println(map);
		Assert.assertEquals("{D=4, A=2, B=1, C=3}", map.toString());
		TreeMap<String, String> treemap = new TreeMap<String, String>();
		treemap.putAll(map);
		System.out.println(treemap);
		Assert.assertNotEquals(treemap.toString(), map.toString());
		Assert.assertEquals("{A=2, B=1, C=3, D=4}", treemap.toString());
		Map<String, String> unmodifiableMapOfTreemap = Collections
				.unmodifiableMap(treemap);
		System.out.println(unmodifiableMapOfTreemap);
		Assert.assertEquals(unmodifiableMapOfTreemap.toString(),
				treemap.toString());

	}

	@Test
	public void testTreeMapSortsActualTokenNames() throws Exception {
		Map<String, String> map = new TreeMap<String, String>();
		map.put("ondev1",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@39157c90");
		map.put("daily1",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@43551d57");
		map.put("daily2",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@51527a1e");
		map.put("ondev3",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@1cbbb253");
		map.put("kiessiToken",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@58433b76");
		map.put("ondev2",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@7303d690");
		map.put("ondev5",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@10ba97c3");
		map.put("ondev4",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@15405f35");
		map.put("jobcontrolledSystem",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@454034");

		String beforeString = "";
		for (Map.Entry<String, String> entry : map.entrySet()) {
			System.out.println(String.format("before=%s entryKey=%s",
					beforeString, entry.getKey()));
			Assert.assertTrue(entry.getKey().compareTo(beforeString) > 0);
			beforeString = entry.getKey();
		}

		System.out.println(map);
	}

	@Test
	public void mapTest() {
		Map<String, String> map = new TreeMap<String, String>();
		map.put("ondev1",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@39157c90");
		map.put("daily1",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@43551d57");
		map.put("daily2",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@51527a1e");
		map.put("ondev3",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@1cbbb253");
		map.put("kiessiToken",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@58433b76");
		map.put("ondev2",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@7303d690");
		map.put("ondev5",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@10ba97c3");
		map.put("ondev4",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@15405f35");
		map.put("jobcontrolledSystem",
				"org.jenkinsci.plugins.tokenplugin.SystemStatusInformation@454034");

		List<String> strings = new LinkedList<String>();
		for (String sys : map.keySet()) {
			strings.add(sys);
		}
		
		for(String delKey : strings)
		{
			map.remove(delKey);
		}
		
		Assert.assertEquals(0, map.size());
	}
}
