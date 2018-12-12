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

package io.frinx.unitopo.unit.xr6.bgp.common

import io.frinx.unitopo.unit.utils.AbstractNetconfHandlerTest
import io.frinx.unitopo.unit.utils.As.Companion.MAX_AS_2BYTE
import io.frinx.unitopo.unit.utils.As.Companion.MAX_AS_4BYTE
import io.frinx.unitopo.unit.utils.As.Companion.asFromDotNotation
import io.frinx.unitopo.unit.utils.As.Companion.asToDotNotation
import org.junit.Assert
import org.junit.Test
import org.opendaylight.yang.gen.v1.http.frinx.openconfig.net.yang.types.inet.rev170403.AsNumber

class AsCommonTest : AbstractNetconfHandlerTest() {

    @Test
    fun testAsTransformation() {
        Assert.assertEquals(asFromDotNotation(0, 1), asFromDotNotation(null, 1))
        Assert.assertEquals(asFromDotNotation(0, 1), AsNumber(1))
        Assert.assertEquals(asFromDotNotation(null, MAX_AS_2BYTE), AsNumber(MAX_AS_2BYTE))
        Assert.assertEquals(asFromDotNotation(1, 0), AsNumber(MAX_AS_2BYTE + 1))
        Assert.assertEquals(asFromDotNotation(1, 1), AsNumber(MAX_AS_2BYTE + 2))
        Assert.assertEquals(asFromDotNotation(478, 654), AsNumber(31326862))
        Assert.assertEquals(asFromDotNotation(111, 0), AsNumber(7274496))
        Assert.assertEquals(asFromDotNotation(111, 1), AsNumber(7274497))
        Assert.assertEquals(asFromDotNotation(MAX_AS_2BYTE, MAX_AS_2BYTE), AsNumber(4294967295))

        Assert.assertEquals(asToDotNotation(AsNumber(MAX_AS_2BYTE)).first, 0)
        Assert.assertEquals(asToDotNotation(AsNumber(MAX_AS_2BYTE)).second, MAX_AS_2BYTE)

        Assert.assertEquals(asToDotNotation(AsNumber(MAX_AS_4BYTE)).first, MAX_AS_2BYTE)
        Assert.assertEquals(asToDotNotation(AsNumber(MAX_AS_4BYTE)).second, MAX_AS_2BYTE)

        Assert.assertEquals(asToDotNotation(AsNumber(MAX_AS_4BYTE)).first, MAX_AS_2BYTE)
        Assert.assertEquals(asToDotNotation(AsNumber(MAX_AS_4BYTE)).second, MAX_AS_2BYTE)

        Assert.assertEquals(asToDotNotation(AsNumber(31326862)).first, 478)
        Assert.assertEquals(asToDotNotation(AsNumber(31326862)).second, 654)

        Assert.assertEquals(asToDotNotation(AsNumber(7274496)).first, 111)
        Assert.assertEquals(asToDotNotation(AsNumber(7274496)).second, 0)

        Assert.assertEquals(asToDotNotation(AsNumber(7274497)).first, 111)
        Assert.assertEquals(asToDotNotation(AsNumber(7274497)).second, 1)
    }
}