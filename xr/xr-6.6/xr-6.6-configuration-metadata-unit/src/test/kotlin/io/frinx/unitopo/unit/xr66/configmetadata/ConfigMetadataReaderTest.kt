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

package io.frinx.unitopo.unit.xr7.configmetadata

import io.fd.honeycomb.translate.read.ReadContext
import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.NetconfAccessHelper
import org.hamcrest.CoreMatchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadata
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.configuration.metadata.rev180731.metadata.ConfigurationMetadataBuilder
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class ConfigMetadataReaderTest : AbstractNetconfHandlerTest() {
    @Mock
    private lateinit var readContext: ReadContext

    private val underlayAccess = NetconfAccessHelper("/config-metadata-oper.xml")
    private val target = ConfigMetadataReader(underlayAccess)

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
    }

    @Test
    fun testReadCurrentAttributes() {
        val id = InstanceIdentifier
            .create(ConfigurationMetadata::class.java)
        val configMetadataBuilder = ConfigurationMetadataBuilder()

        target.readCurrentAttributes(id, configMetadataBuilder, readContext)

        Assert.assertThat(
            configMetadataBuilder.lastConfigurationFingerprint,
            CoreMatchers.equalTo(EXPECTED_FINGERPRINT)
        )
    }

    companion object {
        private const val EXPECTED_FINGERPRINT = "Thu Nov  1 19:54:22 2018"
    }
}