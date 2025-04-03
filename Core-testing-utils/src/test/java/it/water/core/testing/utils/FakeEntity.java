package it.water.core.testing.utils;

import it.water.core.api.model.BaseEntity;

import java.util.Date;

public class FakeEntity implements BaseEntity {

    @Override
    public long getId() {
        return 0;
    }

    @Override
    public Date getEntityCreateDate() {
        return null;
    }

    @Override
    public Date getEntityModifyDate() {
        return null;
    }

    @Override
    public Integer getEntityVersion() {
        return 0;
    }

    @Override
    public void setEntityVersion(Integer entityVersion) {

    }
}
