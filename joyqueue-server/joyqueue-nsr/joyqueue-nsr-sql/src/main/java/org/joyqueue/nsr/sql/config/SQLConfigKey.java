/**
 * Copyright 2019 The JoyQueue Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.joyqueue.nsr.sql.config;

import org.joyqueue.nsr.sql.SQLConsts;
import org.joyqueue.toolkit.config.PropertyDef;

import java.util.HashMap;

/**
 * SQLConfigKey
 * author: gaohaoxiang
 * date: 2019/8/14
 */
public enum SQLConfigKey implements PropertyDef {

    DATASOURCE_TYPE("nameserver.sql.datasource.type", null, Type.STRING),
    DATASOURCE_CLASS("nameserver.sql.datasource.class", "com.zaxxer.hikari.HikariDataSource", Type.STRING),

    DATASOURCE_PROPERTIES_PREFIX("nameserver.sql.datasource.property.", null, Type.STRING),
    DATASOURCE_DEFAULT_PROPERTIES("nameserver.sql.datasource.properties.default", new HashMap<String, String>() {{
        put("nameserver.sql.datasource.property.driverClassName", "org.h2.Driver");
        put("nameserver.sql.datasource.property.jdbcUrl", String.format("jdbc:h2:file:%s/metadata/h2/data;" +
                "AUTO_SERVER=TRUE;MVCC=TRUE;LOCK_TIMEOUT=30000;INIT=RUNSCRIPT FROM '%s'", SQLConsts.APPLICATION_PLACEHOLDER, SQLConsts.DEFAULT_SCHEMA));
    }}, Type.OBJECT),

    ;

    private String name;
    private Object value;
    private Type type;

    SQLConfigKey(String name, Object value, Type type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public Object getValue() {
        return value;
    }

    public Type getType() {
        return type;
    }
}
