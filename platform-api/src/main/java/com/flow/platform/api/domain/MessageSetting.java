/*
 * Copyright 2017 flow.ci
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flow.platform.api.domain;

import com.flow.platform.domain.Jsonable;
import java.time.ZonedDateTime;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * @author yh@firim
 */
@NoArgsConstructor
@EqualsAndHashCode(of = {"id"}, callSuper = false)
@ToString(of = {"id", "createdAt", "updatedAt"})
public class MessageSetting extends Jsonable {

    @Getter
    @Setter
    private SettingContent content;

    @Getter
    @Setter
    private Integer id;

    @Getter
    @Setter
    private String createdBy;

    @Getter
    @Setter
    private ZonedDateTime createdAt;

    @Getter
    @Setter
    private ZonedDateTime updatedAt;

    public MessageSetting(SettingContent content, ZonedDateTime createdAt, ZonedDateTime updatedAt) {
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
