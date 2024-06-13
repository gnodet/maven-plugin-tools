/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.tools.plugin.generator;

import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.descriptor.DuplicateParameterException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.project.MavenProject;
import org.apache.maven.tools.plugin.DefaultPluginToolsRequest;
import org.codehaus.plexus.component.repository.ComponentDependency;
import org.codehaus.plexus.util.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * @author <a href="mailto:jason@maven.org">Jason van Zyl </a>
 */
public abstract class AbstractGeneratorTestCase {
    protected Generator generator;

    protected String basedir = System.getProperty("basedir");

    @Test
    public void testGenerator() throws Exception {
        setupGenerator();

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal("testGoal");
        mojoDescriptor.setImplementation("org.apache.maven.tools.plugin.generator.TestMojo");
        mojoDescriptor.setDependencyResolutionRequired("compile");
        mojoDescriptor.setSince("mojoSince");

        List<Parameter> params = new ArrayList<>();

        Parameter param = new Parameter();
        param.setExpression("${project.build.directory}");
        param.setDefaultValue("</markup-must-be-escaped>");
        param.setName("dir");
        param.setRequired(true);
        param.setType("java.lang.String");
        param.setDescription("Test parameter description");
        param.setAlias("some.alias");
        param.setSince("paramDirSince");
        params.add(param);

        param = new Parameter();
        param.setName("withoutSince");
        param.setType("java.lang.String");
        params.add(param);

        mojoDescriptor.setParameters(params);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        pluginDescriptor.addMojo(mojoDescriptor);

        pluginDescriptor.setArtifactId("maven-unitTesting-plugin");
        pluginDescriptor.setGoalPrefix("test");

        ComponentDependency dependency = new ComponentDependency();
        dependency.setGroupId("testGroup");
        dependency.setArtifactId("testArtifact");
        dependency.setVersion("0.0.0");

        pluginDescriptor.setDependencies(Collections.singletonList(dependency));

        File destinationDirectory =
                Files.createTempDirectory("testGenerator-outDir").toFile();
        destinationDirectory.mkdir();

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("foo");
        mavenProject.setArtifactId("bar");
        Build build = new Build();
        build.setDirectory(basedir + "/target");
        build.setOutputDirectory(basedir + "/target");
        mavenProject.setBuild(build);
        extendPluginDescriptor(pluginDescriptor);
        generator.execute(destinationDirectory, new DefaultPluginToolsRequest(mavenProject, pluginDescriptor));

        validate(destinationDirectory, false);

        FileUtils.deleteDirectory(destinationDirectory);
    }

    @Test
    @Disabled
    public void testGeneratorV4() throws Exception {
        setupGenerator();

        MojoDescriptor mojoDescriptor = new MojoDescriptor();
        mojoDescriptor.setGoal("testGoal");
        mojoDescriptor.setImplementation("org.apache.maven.tools.plugin.generator.TestMojo");
        mojoDescriptor.setDependencyResolutionRequired("compile");
        mojoDescriptor.setSince("mojoSince");

        List<Parameter> params = new ArrayList<>();

        Parameter param = new Parameter();
        param.setExpression("${project.build.directory}");
        param.setDefaultValue("</markup-must-be-escaped>");
        param.setName("dir");
        param.setRequired(true);
        param.setType("java.lang.String");
        param.setDescription("Test parameter description");
        param.setAlias("some.alias");
        param.setSince("paramDirSince");
        params.add(param);

        param = new Parameter();
        param.setName("withoutSince");
        param.setType("java.lang.String");
        params.add(param);

        mojoDescriptor.setParameters(params);

        PluginDescriptor pluginDescriptor = new PluginDescriptor();
        mojoDescriptor.setPluginDescriptor(pluginDescriptor);

        pluginDescriptor.addMojo(mojoDescriptor);

        pluginDescriptor.setArtifactId("maven-unitTesting-plugin");
        pluginDescriptor.setGoalPrefix("test");

        ComponentDependency dependency = new ComponentDependency();
        dependency.setGroupId("testGroup");
        dependency.setArtifactId("testArtifact");
        dependency.setVersion("0.0.0");

        pluginDescriptor.setDependencies(Collections.singletonList(dependency));

        File destinationDirectory =
                Files.createTempDirectory("testGenerator-outDir").toFile();
        destinationDirectory.mkdir();

        MavenProject mavenProject = new MavenProject();
        mavenProject.setGroupId("foo");
        mavenProject.setArtifactId("bar");
        Build build = new Build();
        build.setDirectory(basedir + "/target");
        build.setOutputDirectory(basedir + "/target");
        mavenProject.setBuild(build);
        extendPluginDescriptor(pluginDescriptor);
        DefaultPluginToolsRequest request = new DefaultPluginToolsRequest(mavenProject, pluginDescriptor);
        pluginDescriptor.setRequiredMavenVersion("4.0.0");
        generator.execute(destinationDirectory, request);

        validate(destinationDirectory, true);

        FileUtils.deleteDirectory(destinationDirectory);
    }

    protected void extendPluginDescriptor(PluginDescriptor pluginDescriptor) throws DuplicateParameterException {
        // may be overwritten
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void setupGenerator() throws Exception {
        String generatorClassName = getClass().getName();

        generatorClassName = generatorClassName.substring(0, generatorClassName.length() - 4);

        try {
            Class<?> generatorClass =
                    Thread.currentThread().getContextClassLoader().loadClass(generatorClassName);

            Log log = new SystemStreamLog();
            try {
                Constructor<?> constructor = generatorClass.getConstructor(Log.class);
                generator = (Generator) constructor.newInstance(log);
            } catch (NoSuchMethodException ignore) {
                generator = (Generator) generatorClass.newInstance();
            }
        } catch (Exception e) {
            throw new Exception("Cannot find " + generatorClassName
                    + "! Make sure your test case is named in the form ${generatorClassName}Test "
                    + "or override the setupPlugin() method to instantiate the mojo yourself.");
        }
    }

    // ----------------------------------------------------------------------
    //
    // ----------------------------------------------------------------------

    protected void validate(File destinationDirectory, boolean isV4) throws Exception {
        // empty
    }
}
