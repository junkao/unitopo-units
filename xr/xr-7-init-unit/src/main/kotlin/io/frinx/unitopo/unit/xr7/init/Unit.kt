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

package io.frinx.unitopo.unit.xr7.init

import io.frinx.unitopo.registry.spi.TranslateUnit

/**
 * This unit is a holder of generic settings for XR7 netconf session. No handlers defined here.
 */
abstract class Unit : TranslateUnit {

    override fun toString(): String = "XR 7 init unit"

    // TODO:
    // For some reason transactional feature (means commit after multiple RPC) does not work on XR6.
    // So auto committing is set to true, and every RPC will results a commit.
    // But this is not we really want.
    // When XR7 is implemented, we should do a test of transactional feature,
    // and set auto committing to false if above has been corrected.
    override fun useAutoCommit() = true
}