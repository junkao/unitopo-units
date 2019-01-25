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

package io.frinx.unitopo.unit.xr6.configmetadata

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.xr6.configmetadata.ConfigMetadataReader.Companion.parseTimestamp
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.CfgHistGl
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.cfg.hist.gl.RecordType
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.ios.xr.config.cfgmgr.exec.oper.rev151109.cfg.hist.gl.RecordTypeKey
import org.opendaylight.yang.gen.v1.http.cisco.com.ns.yang.cisco.xr.types.rev150629.CiscoIosXrString
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ConfigMetadataReaderTest : AbstractNetconfHandlerTest() {

    private val DATA_NODES = getResourceAsString("/config-metadata-oper.xml")

    @Test
    fun testGetTimestamp() {
        val recordType = parseGetCfgResponse(DATA_NODES, InstanceIdentifier.create(CfgHistGl::class.java)
            .child(RecordType::class.java, RecordTypeKey(CiscoIosXrString(ConfigMetadataReader.RECORD_TYPE_COMMIT))))
        val lastCommitTime = parseTimestamp(recordType)
        Assert.assertEquals("2018-10-10T11:04:35", lastCommitTime)
    }
}