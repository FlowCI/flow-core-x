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

import com.flow.platform.util.StringUtil;
import java.util.Objects;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yh@firim
 */
@Data
@NoArgsConstructor
public class SearchCondition {

    private String keyword;

    private String branch;

    private String category;

    private String creator;

    public SearchCondition(String keyword, String branch, String gitEventType) {
        this.keyword = keyword;
        this.branch = branch;
        this.category = gitEventType;
    }

    public SearchCondition(String keyword, String branch, String gitEventType, String creator) {
        this.keyword = keyword;
        this.branch = branch;
        this.category = gitEventType;
        this.creator = creator;
    }

    public boolean isEmpty() {
        return StringUtil
            .isNullOrEmptyForItems(getCategory(), getCreator(), getBranch(), getKeyword());
    }

    public static boolean isEmpty(SearchCondition searchCondition) {
        return Objects.isNull(searchCondition) || searchCondition.isEmpty();
    }

}
