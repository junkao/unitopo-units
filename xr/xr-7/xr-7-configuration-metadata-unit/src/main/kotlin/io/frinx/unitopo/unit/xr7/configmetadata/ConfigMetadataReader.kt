/*
 * Copyright © 2020 Frinx and others.
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

package io.frinx.unitopo.unit.xr7.configmetadata

import com.google.common.base.Optional
import io.fd.honeycomb.translate.read.ReadContext
import io.fd.honeycomb.translate.spi.read.OperReaderCustomizer
import io.frinx.unitopo.registry.spi.UnderlayAccess
import org.apache.commons.lang3.time.FastDateFormat
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev190405.ConfigManager
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev190405.config.manager.Global
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev190405.config.manager.global.ConfigCommit
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev190405.config.manager.global.config.commit.Commits
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadata
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadataBuilder
import org.opendaylight.yangtools.concepts.Builder
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier
import org.slf4j.LoggerFactory
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
        getCommitHistory(0).orNull()
            ?.commit.orEmpty()
            .maxBy { parseDateFormat(it.timestamp) }
            ?.let { configmetadata.lastConfigurationFingerprint = it.timestamp }
    }

    private fun getCommitHistory(attempts: Int): Optional<Commits> {
        val future = underlayAccess.read(COMMITS_IID, LogicalDatastoreType.OPERATIONAL)
        return try {
            future.checkedGet()
        } catch (e: ReadFailedException) {
            val updatedAttempts = attempts + 1
            if (updatedAttempts >= MAX_READ_METADATA_ATTEMPTS) {
                throw e
            }
            LOG.warn("Cannot read commit records from device (attempt {}), trying again.", updatedAttempts, e)
            getCommitHistory(updatedAttempts)
        }
    }

    override fun merge(
        parentBuilder: Builder<out DataObject>,
        data: ConfigurationMetadata
    ) {
        (parentBuilder as ConfigurationMetadataBuilder).lastConfigurationFingerprint =
            data.lastConfigurationFingerprint
    }

    companion object {
        private const val MAX_READ_METADATA_ATTEMPTS = 4
        private val LOG = LoggerFactory.getLogger(ConfigMetadataReader::class.java)

        private val COMMITS_IID = InstanceIdentifier.create(ConfigManager::class.java)
            .child(Global::class.java)
            .child(ConfigCommit::class.java)
            .child(Commits::class.java)

        private val DATE_FORMAT = FastDateFormat.getInstance("EEE MMM dd HH:mm:ss yyyy", Locale.ENGLISH)

        private fun parseDateFormat(timestamp: String): Date =
            DATE_FORMAT.parse(timestamp)
    }
}