/*
 * Copyright 2024 Ant Group
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alipay.antchain.bridge.relayer.facade.admin.types;

import com.alibaba.fastjson.annotation.JSONField;
import com.alipay.antchain.bridge.commons.core.ptc.PTCTypeEnum;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PtcServiceInfo {

    /**
     * The id of the ptc service.
     */
    @JSONField(name = "serviceId")
    private String ptcServiceId;

    /**
     * Means that this ptc service is the default one.
     */
    private boolean asDefault;

    @JSONField(name = "type")
    private PTCTypeEnum ptcType;

    private String description;
}
