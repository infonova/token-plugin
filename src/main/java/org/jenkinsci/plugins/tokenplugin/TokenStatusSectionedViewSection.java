package org.jenkinsci.plugins.tokenplugin;

import hudson.Extension;
import hudson.model.Hudson;
import hudson.plugins.sectioned_view.SectionedViewSection;
import hudson.plugins.sectioned_view.SectionedViewSectionDescriptor;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
    public Set<Entry<String,SystemStatusInformation>> getToken() {
        DescriptorImpl descriptor = Hudson.getInstance().getDescriptorByType(ManageTokenBuilder.DescriptorImpl.class);

        Set<Map.Entry<String,SystemStatusInformation>> filteredTokens = new HashSet<Entry<String, SystemStatusInformation>>();
        for (Map.Entry<String,SystemStatusInformation> entry : descriptor.getSystems().entrySet()) {
            if (Pattern.matches(getIncludeRegex(), (CharSequence)entry.getKey())) {
                filteredTokens.add(entry);
            }
        }
        return filteredTokens;
    }

}
