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

package com.flowci.core.test.plugin;

import com.flowci.core.plugin.domain.Plugin;
import com.flowci.core.plugin.domain.PluginParser;
import com.flowci.domain.Input;
import com.flowci.domain.VarType;
import com.flowci.domain.Version;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author yang
 */
public class PluginParserTest {

    @Test
    void should_parse_yml_to_plugin() {
        InputStream is = PluginParserTest.class.getClassLoader().getResourceAsStream("plugin.yml");
        Plugin.Meta meta = PluginParser.parse(is);
        assertNotNull(meta);

        assertEquals(1, meta.getMatrixTypes().size());
        assertEquals("gitclone", meta.getName());
        assertEquals(Version.of(0, 0, 1, null), meta.getVersion());
        assertEquals("src/icon.svg", meta.getIcon());

        List<Input> inputs = meta.getInputs();
        assertEquals(4, inputs.size());

        Input varForTimeout = inputs.get(3);
        assertNotNull(varForTimeout);
        assertEquals("GIT_TIMEOUT", varForTimeout.getName());
        assertEquals(VarType.INT, varForTimeout.getType());
        assertEquals(60, varForTimeout.getIntDefaultValue());

        Set<String> exports = meta.getExports();
        assertEquals(2, exports.size());
        assertTrue(exports.contains("VAR_EXPORT_1"));
        assertTrue(exports.contains("VAR_EXPORT_2"));

        String pwsh = meta.getPwsh();
        assertNotNull(pwsh);
        assertEquals("$Env.PK_FILE=keyfile", pwsh.trim());

        String bash = meta.getBash();
        assertNotNull(bash);
        assertEquals("chmod 400 ${PK_FILE}", bash.trim());
    }
}
