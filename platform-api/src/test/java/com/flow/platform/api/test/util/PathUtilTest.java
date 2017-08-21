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

package com.flow.platform.api.test.util;

import com.flow.platform.api.util.PathUtil;
import com.flow.platform.core.exception.IllegalParameterException;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author yang
 */
public class PathUtilTest {

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_start_with_slash() {
        PathUtil.validateName("/hello");
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_less_than_min_length() {
        PathUtil.validateName("h");
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_contains_start() {
        PathUtil.validateName("hello*ss");
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_path_only_slash() {
        PathUtil.validatePath("/");
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_path_without_slash() {
        PathUtil.validatePath("hello");
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_path_end_with_slash() {
        PathUtil.validatePath("/hello/");
    }

    @Test(expected = IllegalParameterException.class)
    public void should_raise_exception_if_path_over_max_depth() {
        PathUtil.validatePath("/hello/hello/hello/hello/hello/hello/hello/hello/hello/hello/hello");
    }

    @Test
    public void should_build_node_path_and_get_right_info() {
        // verify single node path
        String path = PathUtil.build("root");
        Assert.assertEquals("/root", path);

        Assert.assertEquals("root", PathUtil.rootName(path));
        Assert.assertEquals("/root", PathUtil.rootPath(path));

        Assert.assertEquals("", PathUtil.parentName(path));
        Assert.assertEquals("", PathUtil.parentPath(path));

        Assert.assertEquals("root", PathUtil.currentName(path));

        // verify multiple node path
        path = PathUtil.build("root", "/child", "sub");
        Assert.assertEquals("/root/child/sub", path);

        Assert.assertEquals("root", PathUtil.rootName(path));
        Assert.assertEquals("/root", PathUtil.rootPath(path));

        Assert.assertEquals("child", PathUtil.parentName(path));
        Assert.assertEquals("/root/child", PathUtil.parentPath(path));

        Assert.assertEquals("sub", PathUtil.currentName(path));

        // verify unformatted path build
        path = PathUtil.build("/root/child/", "sub");
        Assert.assertEquals("/root/child/sub", path);

        path = PathUtil.build("root/child/", "/sub/");
        Assert.assertEquals("/root/child/sub", path);

        path = PathUtil.build("", "/sub/");
        Assert.assertEquals("/sub", path);

        path = PathUtil.build(null, "/sub/");
        Assert.assertEquals("/sub", path);

        path = PathUtil.build("sub", null, null);
        Assert.assertEquals("/sub", path);
    }
}
