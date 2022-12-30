/*
 * Copyright 2018 flow.ci
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

package com.flowci.core.flow.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flowci.core.common.domain.Mongoable;
import com.flowci.util.ObjectsHelper;
import com.flowci.util.StringHelper;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.internal.util.CollectionHelper;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;
import java.util.Objects;

/**
 * @author yang
 */
@Getter
@Setter
@Document(collection = "flow_yml")
@NoArgsConstructor
@AllArgsConstructor
@CompoundIndexes(
        @CompoundIndex(
                name = "index_yml_name",
                def = "{'list.name': 1}",
                sparse = true,
                unique = true
        )
)
public class FlowYml extends Mongoable {

    public final static String DEFAULT_NAME = ".flowci.yml";

    @Indexed(name = "index_yml_flow_id", unique = true)
    private String flowId;

    private List<SimpleYml> list;

    public boolean hasYml() {
        return ObjectsHelper.hasCollection(list);
    }

    public static String[] toRawArray(List<SimpleYml> list) {
        var array = new String[list.size()];
        int i = 0;
        for (var item : list) {
            array[0] = StringHelper.fromBase64(item.getRawInB64());
        }
        return array;
    }
}
