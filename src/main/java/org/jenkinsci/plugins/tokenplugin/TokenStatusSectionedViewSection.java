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

import org.jenkinsci.plugins.tokenplugin.ManageTokenBuilder.DescriptorImpl;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.export.Exported;


public class TokenStatusSectionedViewSection extends SectionedViewSection {

    @DataBoundConstructor
    public TokenStatusSectionedViewSection(String name, Width width, Positioning alignment) {
        super(name, width, alignment);
    }


    @Extension
    public static final class TokenStatusSectionedViewSectionDescriptorImpl extends SectionedViewSectionDescriptor {

        @Override
        public String getDisplayName() {
            return "TokenStatusSection";
        }
    }

    @Exported
    public List<Entry<String,SystemStatusInformation>> getToken() {
        Hudson.getInstance().getDescriptorByType(ManageTokenBuilder.DescriptorImpl.class);

        List<Map.Entry<String,SystemStatusInformation>> filteredTokens = new ArrayList<Entry<String, SystemStatusInformation>>();
        Map<String, SystemStatusInformation> systems = new TreeMap<String, SystemStatusInformation>();
        systems.putAll(TokenManager.getSystems());
        for (Map.Entry<String,SystemStatusInformation> entry : systems.entrySet()) {
            if (Pattern.matches(getIncludeRegex(), entry.getKey())) {
                filteredTokens.add(entry);
            }
        }
        return filteredTokens;
    }

}
