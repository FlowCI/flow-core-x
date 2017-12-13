package com.flow.platform.plugin.test.util;

import com.flow.platform.plugin.domain.Plugin;
import com.flow.platform.plugin.util.FileUtil;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.*;

/**
 * @author yh@firim
 */
public class FileUtilTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void should_save_success() throws IOException {
        File file = temporaryFolder.newFolder("test");
        Path basePath = file.toPath();

        Path filePath = Paths.get(basePath.toString(), ".cache");

        Plugin plugin = new Plugin("test", "test", Sets.newHashSet("1", "2"), "hello@flow.ci", Sets.newHashSet("a"));
        FileUtil.write(plugin, filePath);

        Plugin rPlugin = FileUtil.read(Plugin.class, filePath);
        Assert.assertEquals(plugin.getName(), rPlugin.getName());
        cleanFile(basePath);
    }

    @Test
    public void should_save_success_demo_too() throws IOException {
        File file = temporaryFolder.newFolder("test");
        Path basePath = file.toPath();

        Path filePath = Paths.get(basePath.toString(), ".cache");

        Map<String, Plugin> map = new HashMap<>();
        Plugin plugin = new Plugin("test", "test", Sets.newHashSet("1", "2"), "hello@flow.ci", Sets.newHashSet("a"));
        map.put(plugin.getName(), plugin);
        FileUtil.write(map, filePath);

        Type type = new TypeToken<Map<String, Plugin>>(){}.getType();

        Map<String, Plugin> rMap = FileUtil.read(type, filePath);
        Assert.assertEquals(plugin.getName(), rMap.get(plugin.getName()).getName());

        cleanFile(basePath);
    }

    private void cleanFile(Path path) throws IOException {
        FileUtils.deleteDirectory(path.toFile());
    }
}