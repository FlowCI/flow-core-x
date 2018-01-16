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

import com.flow.platform.util.StringUtil;
import java.util.Objects;

/**
 * @author gyfirim
 */
public class Pageable{

    public final static int DEFAULT_NUMBER = 1;

    public final static int DEFAULT_SIZE = 20;

    private int number;

    private int size;

    public Pageable(int number, int size) {
        this.number = number;
        this.size = size;
    }

    public Pageable (){

    }

    public int getPageNumber() {
        return number;
    }

    public int getPageSize() {
        return size;
    }

    public int getOffset() {
        return (number-1) * size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public static boolean isEmpty(Pageable pageable) {
        return Objects.isNull(pageable) || StringUtil
            .isNullOrEmptyForItems(String.valueOf(pageable.getPageNumber()),
                String.valueOf(pageable.getPageSize())) ||
            (pageable.getPageSize() == 0 || pageable.getPageNumber() == 0);
    }

}
