package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.plugins.sectioned_view.SectionedViewSection;
import hudson.plugins.sectioned_view.SectionedViewSectionDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;

public class TokenStatusSectionedViewSection extends SectionedViewSection {

	private TokenPluginConfig tokenConfig = null;

	@DataBoundConstructor
	public TokenStatusSectionedViewSection(String name, Width width,
			Positioning alignment) {
		super(name, width, alignment);
	}

	@Extension
	public static final class TokenStatusSectionedViewSectionDescriptorImpl
			extends SectionedViewSectionDescriptor {

		@Override
		public String getDisplayName() {
			return "TokenStatusSection";
		}
	}

	@Exported
	public List<Entry<String, SystemStatusInformation>> getToken() {
		Hudson.getInstance().getDescriptorByType(
				ManageTokenBuilder.DescriptorImpl.class);

		List<Map.Entry<String, SystemStatusInformation>> filteredTokens = new ArrayList<Entry<String, SystemStatusInformation>>();
		Map<String, SystemStatusInformation> systems = new TreeMap<String, SystemStatusInformation>();
		systems.putAll(TokenManager.getInstance().getSystems());
		for (Map.Entry<String, SystemStatusInformation> entry : systems
				.entrySet()) {
			if (Pattern.matches(getIncludeRegex(), entry.getKey())) {
				filteredTokens.add(entry);
			}
		}
		return filteredTokens;
	}

	private void initConifg() {
		if (tokenConfig == null) {
			tokenConfig = new TokenPluginConfig();
		} else {
			// load configfile
			tokenConfig.load();
		}

	}

	public String getDeletejob() {
		initConifg();
		return tokenConfig.getLinkToUtilityDeleteJob();
	}

	public String getLockjob() {
		initConifg();
		return tokenConfig.getLinkToUtilityLockSystemJob();
	}

	public String getUnlockjob() {
		initConifg();
		return tokenConfig.getLinkToUtilityUnlockSystemJob();
	}

	public String getSetheaderlinkjob() {
		initConifg();
		return tokenConfig.getLinkToUtilitySetheaderlinkJob();
	}

}
