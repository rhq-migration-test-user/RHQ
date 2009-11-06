/*
 * RHQ Management Platform
 * Copyright (C) 2005-2009 Red Hat, Inc.
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
package org.rhq.enterprise.server.plugin.pc;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.enterprise.server.plugin.pc.alert.AlertServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.content.ContentServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.generic.GenericServerPluginContainer;
import org.rhq.enterprise.server.plugin.pc.perspective.PerspectiveServerPluginContainer;
import org.rhq.enterprise.server.xmlschema.ServerPluginDescriptorUtil;
import org.rhq.enterprise.server.xmlschema.generated.serverplugin.ServerPluginDescriptorType;

/**
 * The container responsible for managing all the plugin containers for all the
 * different plugin types.
 *
 * @author John Mazzitelli
 */
public class MasterServerPluginContainer {
    private static final Log log = LogFactory.getLog(MasterServerPluginContainer.class);

    private MasterServerPluginContainerConfiguration configuration;
    private Map<ServerPluginType, AbstractTypeServerPluginContainer> pluginContainers = new HashMap<ServerPluginType, AbstractTypeServerPluginContainer>();
    private ClassLoaderManager classLoaderManager;

    /**
     * Starts the master plugin container, which will load all plugins and begin managing them.
     *
     * @param config the master configuration
     */
    public void initialize(MasterServerPluginContainerConfiguration config) {
        try {
            log.debug("Master server plugin container has been initialized with config: " + config);

            this.configuration = config;

            // load all server-side plugins - this just parses their descriptors and confirms they are valid server plugins
            Map<URL, ? extends ServerPluginDescriptorType> plugins = preloadAllPlugins();

            // create the root classloader to be used as the top classloader for all plugins
            ClassLoader rootClassLoader = createRootServerPluginClassLoader();
            File tmpDir = this.configuration.getTemporaryDirectory();
            this.classLoaderManager = createClassLoaderManager(plugins, rootClassLoader, tmpDir);

            // create all known child plugin containers and map them to their supported plugin types
            List<AbstractTypeServerPluginContainer> pcs = createPluginContainers();
            for (AbstractTypeServerPluginContainer pc : pcs) {
                this.pluginContainers.put(pc.getSupportedServerPluginType(), pc);
            }

            // initialize all the plugin containers
            for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : this.pluginContainers
                .entrySet()) {
                log.info("Master PC is initializing plugin container for plugin type [" + entry.getKey() + "]");
                try {
                    entry.getValue().initialize();
                } catch (Exception e) {
                    log.warn("Failed to initialize plugin container for plugin type [" + entry.getKey() + "]", e);
                }
            }

            // create classloaders for all plugins and load plugins into their plugin containers
            // Note that we do not care what order we load plugins - in the future we may want dependencies
            for (Map.Entry<URL, ? extends ServerPluginDescriptorType> entry : plugins.entrySet()) {
                URL pluginUrl = entry.getKey();
                ServerPluginDescriptorType descriptor = entry.getValue();
                String pluginName = entry.getValue().getName();
                ClassLoader parentCL = this.classLoaderManager.obtainServerPluginClassLoader(pluginName);
                ServerPluginEnvironment env = new ServerPluginEnvironment(pluginUrl, parentCL, tmpDir, descriptor);
                AbstractTypeServerPluginContainer pc = getPluginContainerByDescriptor(descriptor);
                if (pc != null) {
                    log.debug("Loading plugin [" + pluginUrl + "] into its plugin container");
                    pc.loadPlugin(env);
                } else {
                    log.warn("Failed to get a plugin container for plugin: " + pluginUrl);
                }
            }

        } catch (Throwable t) {
            shutdown();
            log.error("Failed to initialize master plugin container! Server side plugins will not start.", t);
        }

        return;
    }

    /**
     * Stops all plugins and cleans up after them.
     */
    public void shutdown() {
        log.debug("Master server plugin container is being shutdown");

        // shutdown all the plugin containers which in turn shuts down all their plugins
        for (Map.Entry<ServerPluginType, AbstractTypeServerPluginContainer> entry : this.pluginContainers.entrySet()) {
            log.info("Master PC is shutting down plugin container for plugin type [" + entry.getKey() + "]");
            try {
                entry.getValue().shutdown();
            } catch (Exception e) {
                log.error("Failed to shutdown plugin container for plugin type [" + entry.getKey() + "]", e);
            }
        }

        // now shutdown the classloader manager, destroying the classloaders it created
        this.classLoaderManager.shutdown();

        this.pluginContainers.clear();
        this.classLoaderManager = null;
        this.configuration = null;
    }

    /**
     * Returns the configuration that this object was initialized with. If this plugin container was not
     * {@link #initialize(MasterServerPluginContainerConfiguration) initialized} or has been {@link #shutdown() shutdown},
     * this will return <code>null</code>.
     *
     * @return the configuration
     */
    public MasterServerPluginContainerConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Get the plugin container of the given class. This method provides a strongly typed return value,
     * based on the type of plugin container the caller wants returned.
     * 
     * @param clazz the class name of the plugin container that the caller wants
     * @return the plugin container of the given class (<code>null</code> if none found)
     */
    public <T extends AbstractTypeServerPluginContainer> T getPluginContainer(Class<T> clazz) {
        for (AbstractTypeServerPluginContainer pc : this.pluginContainers.values()) {
            if (clazz.isInstance(pc)) {
                return (T) pc;
            }
        }
        return null;
    }

    /**
     * Returns the manager that is responsible for created classloaders for plugins.
     * 
     * @return classloader manager
     */
    public ClassLoaderManager getClassLoaderManager() {
        return this.classLoaderManager;
    }

    /**
     * Given a plugin's descriptor, this will return the plugin container that can manage the plugin.
     * 
     * @param descriptor descriptor to identify a plugin whose container is to be returned
     * @return a plugin container that can handle the plugin with the given descriptor
     */
    protected AbstractTypeServerPluginContainer getPluginContainerByDescriptor(ServerPluginDescriptorType descriptor) {
        ServerPluginType pluginType = new ServerPluginType(descriptor.getClass());
        AbstractTypeServerPluginContainer pc = this.pluginContainers.get(pluginType);
        return pc;
    }

    /**
     * Finds all plugins and parses their descriptors.
     * 
     * If a plugin fails to load, it will be ignored - other plugins will still load.
     * 
     * @return a map of plugins, keyed on the plugin jar URL whose values are the parsed descriptors
     *
     * @throws Exception on catastrophic failure. Note that if a plugin failed to load,
     *                   that plugin will simply be ignored and no exception will be thrown
     */
    protected Map<URL, ? extends ServerPluginDescriptorType> preloadAllPlugins() throws Exception {
        Map<URL, ServerPluginDescriptorType> plugins;

        plugins = new HashMap<URL, ServerPluginDescriptorType>();

        File[] pluginFiles = this.configuration.getPluginDirectory().listFiles();
        for (File pluginFile : pluginFiles) {
            if (pluginFile.getName().endsWith(".jar")) {
                URL pluginUrl = pluginFile.toURI().toURL();

                try {
                    ServerPluginDescriptorType descriptor;
                    descriptor = ServerPluginDescriptorUtil.loadPluginDescriptorFromUrl(pluginUrl);
                    if (descriptor != null) {
                        log.debug("pre-loaded server plugin from URL: " + pluginUrl);
                        plugins.put(pluginUrl, descriptor);
                    }
                } catch (Throwable t) {
                    // for some reason, the plugin failed to load - it will be ignored
                    log.error("Plugin at [" + pluginUrl + "] could not be pre-loaded. Ignoring it.", t);
                }
            }
        }

        return plugins;
    }

    /**
     * Creates the individual plugin containers that can be used to deploy different plugin types.
     * 
     * <p>This is protected to allow subclasses to override the PCs that are created by this service (mainly to support tests).</p>
     * 
     * @return the new plugin containers created by this method
     */
    protected List<AbstractTypeServerPluginContainer> createPluginContainers() {
        ArrayList<AbstractTypeServerPluginContainer> pcs = new ArrayList<AbstractTypeServerPluginContainer>(4);
        pcs.add(new GenericServerPluginContainer(this));
        pcs.add(new ContentServerPluginContainer(this));
        pcs.add(new PerspectiveServerPluginContainer(this));
        pcs.add(new AlertServerPluginContainer(this));
        return pcs;
    }

    /**
     * Create the root classloader that will be the ancester to all plugin classloaders.
     * 
     * @return the root server plugin classloader
     */
    protected ClassLoader createRootServerPluginClassLoader() {
        ClassLoader thisClassLoader = this.getClass().getClassLoader();
        String classesToHideRegexStr = this.configuration.getRootServerPluginClassLoaderRegex();
        RootServerPluginClassLoader root = new RootServerPluginClassLoader(null, thisClassLoader, classesToHideRegexStr);
        return root;
    }

    /**
     * Creates the manager that will be responsible for instantiating plugin classloaders.
     * @param plugins maps plugin URLs with their parsed descriptors
     * @param rootClassLoader the classloader at the top of the classloader hierarchy
     * @param tmpDir where the classloaders can write out the jars that are embedded in the plugin jars
     * 
     * @return the classloader manager instance
     */
    protected ClassLoaderManager createClassLoaderManager(Map<URL, ? extends ServerPluginDescriptorType> plugins,
        ClassLoader rootClassLoader, File tmpDir) {
        return new ClassLoaderManager(plugins, rootClassLoader, tmpDir);
    }
}