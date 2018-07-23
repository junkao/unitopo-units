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

package io.frinx.unitopo.unit.xr6.init

import io.frinx.unitopo.registry.spi.TranslateUnit

/**
 * This unit is a holder of generic settings for XR6 netconf session. No handlers defined here.
 */
abstract class Unit : TranslateUnit {

    override fun toString(): String = "XR 6 init unit"

    override fun useAutoCommit() = true
}