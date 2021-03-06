/*
 * RHQ Management Platform
 * Copyright (C) 2005-2010 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package org.rhq.enterprise.server.bundle;

import java.util.Set;

import org.rhq.core.clientapi.agent.configuration.ConfigurationUtility;
import org.rhq.core.domain.configuration.definition.ConfigurationDefinition;
import org.rhq.core.util.updater.DeploymentProperties;

/**
 * This class is general to all Bundle handling server side plugins.  It is returned by the SSP when parsing
 * a bundle's recipe and contains all of the information neded to create a bundle version.  
 * 
 * @author Jay Shaughnessy
 * @author John Mazzitelli
 */
public class RecipeParseResults {

    /** information about the bundle, including name, version and description */
    private DeploymentProperties bundleMetadata;

    /** The configuration definition parsed out of, or expcitly provided by, the recipe */
    private ConfigurationDefinition configDef;

    /** The set of bundle files that make up the bundle (version) as specified in the recipe commands */
    private Set<String> bundleFileNames;

    public RecipeParseResults(DeploymentProperties bundleMetadata, ConfigurationDefinition configDef,
        Set<String> bundleFileNames) {
        setBundleMetadata(bundleMetadata);
        setConfigurationDefinition(configDef);
        setBundleFiles(bundleFileNames);
    }

    public DeploymentProperties getBundleMetadata() {
        return bundleMetadata;
    }

    public void setBundleMetadata(DeploymentProperties bundleMetadata) {
        this.bundleMetadata = bundleMetadata;
    }

    public ConfigurationDefinition getConfigurationDefinition() {
        return configDef;
    }

    public void setConfigurationDefinition(ConfigurationDefinition configDef) {
        this.configDef = configDef;
        ConfigurationUtility.initializeDefaultTemplate(this.configDef);
    }

    public Set<String> getBundleFileNames() {
        return bundleFileNames;
    }

    public void setBundleFiles(Set<String> bundleFileNames) {
        this.bundleFileNames = bundleFileNames;
    }
}
