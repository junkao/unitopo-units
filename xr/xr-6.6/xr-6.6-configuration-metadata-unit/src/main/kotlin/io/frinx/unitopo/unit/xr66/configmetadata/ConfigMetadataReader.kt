/*
 * Copyright © 2018 Frinx and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.frinx.unitopo.unit.xr66.configmetadata

import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.apache.commons.lang3.time.FastDateFormat
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev170907.ConfigManager
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev170907.config.manager.Global
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev170907.config.manager.global.ConfigCommit
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev170907.config.manager.global.config.commit.Commits
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev170907.config.manager.global.config.commit.CommitsBuilder
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadata
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadataBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import java.util.Date
import java.util.Locale

class ConfigMetadataReader(private val underlayAccess: UnderlayAccess) :
    OperReaderCustomizer<ConfigurationMetadata, ConfigurationMetadataBuilder> {

    override fun getBuilder(id: InstanceIdentifier<ConfigurationMetadata>): ConfigurationMetadataBuilder =
        ConfigurationMetadataBuilder()

    override fun readCurrentAttributes(
        id: InstanceIdentifier<ConfigurationMetadata>,
        configmetadata: ConfigurationMetadataBuilder,
        ctx: ReadContext
    ) {
        underlayAccess.read(COMMITS_IID, LogicalDatastoreType.OPERATIONAL)
            .checkedGet()
            .orNull()
            ?.commit.orEmpty()
            .maxBy { parseDateFormat(it.timestamp) }
            ?.let { configmetadata.lastConfigurationFingerprint = it.timestamp }
    }

    override fun merge(
        parentBuilder: Builder<out DataObject>,
        data: ConfigurationMetadata
    ) {
        (parentBuilder as ConfigurationMetadataBuilder).lastConfigurationFingerprint =
            data.lastConfigurationFingerprint
    }

    companion object {
        val COMMITS_IID = InstanceIdentifier.create(ConfigManager::class.java)
            .child(Global::class.java)
            .child(ConfigCommit::class.java)
            .child(Commits::class.java)

        val COMMITS_EMPTY = CommitsBuilder().build()

        private val DATE_FORMAT = FastDateFormat.getInstance("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH)

        private fun parseDateFormat(timestamp: String): Date =
            DATE_FORMAT.parse(timestamp)
    }
}