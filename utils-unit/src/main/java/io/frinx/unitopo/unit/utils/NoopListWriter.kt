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

package io.frinx.unitopo.unit.utils

import io.fd.honeycomb.translate.spi.write.ListWriterCustomizer
import io.fd.honeycomb.translate.write.WriteContext
import org.opendaylight.yangtools.yang.binding.DataObject
import org.opendaylight.yangtools.yang.binding.Identifiable
import org.opendaylight.yangtools.yang.binding.Identifier
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier

class NoopListWriter<C, K> : ListWriterCustomizer<C, K>
        where C: DataObject, C: Identifiable<K>, K: Identifier<C> {

    override fun writeCurrentAttributes(p0: InstanceIdentifier<C>, p1: C, p2: WriteContext) {}

    override fun deleteCurrentAttributes(p0: InstanceIdentifier<C>, p1: C, p2: WriteContext) {}

}