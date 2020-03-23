/*
 *   Copyright (c) 2019 flow.ci
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.flowci.domain;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.Base64;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author yang
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CmdId implements Serializable {

    public static CmdId parse(String id) {
        try {
            byte[] decode = Base64.getDecoder().decode(id);
            String idString = new String(decode);
            int index = idString.indexOf('-');

            return new CmdId(idString.substring(0, index), idString.substring(index + 1));
        } catch (Throwable e) {
            return null;
        }
    }

    private String jobId;

    private String nodePath;

    @Override
    public String toString() {
        String cmdId = MessageFormat.format("{0}-{1}", jobId, nodePath);
        return Base64.getEncoder().encodeToString(cmdId.getBytes());
    }
}
