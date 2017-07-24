/*
 * Copyright (C) 2017 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.blackducksoftware.integration.hub.detect.bomtool.sbt

import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.blackducksoftware.integration.hub.bdio.simple.model.DependencyNode
import com.blackducksoftware.integration.hub.bdio.simple.model.externalid.MavenExternalId
import com.blackducksoftware.integration.hub.detect.bomtool.sbt.models.SbtConfigurationDependencyTree

public class SbtConfigurationAggregator {
    private final Logger logger = LoggerFactory.getLogger(SbtConfigurationAggregator.class)

    DependencyNode aggregateConfigurations(List<SbtConfigurationDependencyTree> configurations) {
        def name = findSharedName(configurations)
        def org = findSharedOrg(configurations)
        def version = findSharedVersion(configurations)

        if (name == null || org == null || version == null) {
            return null
        }

        DependencyNode root = new DependencyNode(new MavenExternalId(org, name, version))
        root.name = name
        root.version = version
        root.children = new ArrayList<DependencyNode>()
        configurations.each {config ->
            root.children += config.rootNode.children
        }

        return root
    }

    String findSharedName(List<SbtConfigurationDependencyTree> configurations) {
        def names = configurations.collect { config -> config.rootNode.name }
        firstUniqueOrLogError(names, "configuration name")
    }

    String findSharedOrg(List<SbtConfigurationDependencyTree> configurations) {
        def orgs = configurations.collect { config ->
            def id = config.rootNode.externalId as MavenExternalId
            id.group
        }
        firstUniqueOrLogError(orgs, "organisation")
    }

    String findSharedVersion(List<SbtConfigurationDependencyTree> configurations) {
        def versions = configurations.collect { config -> config.rootNode.version }
        firstUniqueOrLogError(versions, "version")
    }

    String firstUniqueOrLogError(List<String> things, String thingType) {
        def uniqueThings = things.toUnique()
        def result = null
        if (uniqueThings.size == 1) {
            result = uniqueThings.first()
        } else if (uniqueThings.size == 0) {
            logger.error("Could not find any ${thingType} in ivy reports!")
        } else if (uniqueThings.size > 1) {
            logger.error("Found more than 1 unique ${thingType} in ivy reports: ${uniqueThings}!")
        }
        result
    }
}