/*
 * Copyright 2024 Aristide Cittadino
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package it.water.core.api.model;

import lombok.Setter;

import java.util.Date;

@Setter
public class TestEntity implements BaseEntity {
    private long id;
    private Date entityCreateDate;
    private Date entityModifyDate;
    private Integer entityVersion;

    public TestEntity(long id, Date entityCreateDate, Date entityModifyDate, Integer entityVersion) {
        this.id = id;
        this.entityCreateDate = entityCreateDate;
        this.entityModifyDate = entityModifyDate;
        this.entityVersion = entityVersion;
    }

    public TestEntity() {
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Date getEntityCreateDate() {
        return entityCreateDate;
    }

    @Override
    public Date getEntityModifyDate() {
        return entityModifyDate;
    }

    @Override
    public Integer getEntityVersion() {
        return entityVersion;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {
        this.entityVersion = entityVersion;
    }
}
