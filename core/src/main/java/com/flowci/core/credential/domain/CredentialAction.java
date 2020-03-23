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

package com.flowci.core.credential.domain;

public abstract class CredentialAction {

    public static final String LIST = "list_credential";

    public static final String LIST_NAME = "list_credential_name_only";

    public static final String GET = "get_credential";

    public static final String CREATE_RSA = "create_credential_rsa";

    public static final String CREATE_AUTH = "create_credential_auth";

    public static final String GENERATE_RSA = "generate_rsa";

    public static final String DELETE = "delete_credential";

    public static final String[] ALL = {
        LIST,
        LIST_NAME,
        GET,
        CREATE_RSA,
        CREATE_AUTH,
        GENERATE_RSA,
        DELETE
    };

}
