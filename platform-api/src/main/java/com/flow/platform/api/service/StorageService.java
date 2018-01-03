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

package com.flow.platform.api.service;

import com.flow.platform.api.domain.Storage;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author yh@firim
 */
public interface StorageService {

    Storage get(String id);

    Storage save(Storage storage);

    Storage update(Storage storage);

    Storage delete(Storage storage);

    Storage create(MultipartFile file);

    Resource getResource(String id);
}
