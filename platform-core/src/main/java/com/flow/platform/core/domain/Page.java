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

package com.flow.platform.core.domain;

import com.flow.platform.core.dao.AbstractBaseDao.TotalSupplier;
import com.flow.platform.domain.Jsonable;
import com.google.gson.annotations.Expose;
import java.util.Collections;
import java.util.List;

/**
 * @author gyfirim
 */
public class Page<T> extends Jsonable {

    @Expose
    private List<T> content = Collections.emptyList();

    @Expose
    private long totalSize;

    // page index
    @Expose
    private int pageNumber;

    private int pageSize;

    private int pageCount;

    public Page(List<T> content, int pageSize, int number, TotalSupplier totalSupplier) {
        this.content = content;
        this.totalSize = totalSupplier.get();
        this.pageSize = pageSize;
        this.pageNumber = number;
        this.pageCount = pageSize == 0 ? 1 : (int) Math.ceil((double) this.totalSize / (double)pageSize);
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public long getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(long totalSize) {
        this.totalSize = totalSize;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPageCount() {
        return pageCount;
    }

    public void setPageCount(int pageCount) {
        this.pageCount = pageCount;
    }
}
