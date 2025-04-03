package it.water.core.testing.utils;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.interceptors.annotations.FrameworkComponent;

@FrameworkComponent
public class FakeSystemApi implements TestSystemApi {
    @Override
    public FakeEntity save(FakeEntity entity) {
        return null;
    }

    @Override
    public FakeEntity update(FakeEntity entity) {
        return null;
    }

    @Override
    public void remove(long id) {

    }

    @Override
    public FakeEntity find(long id) {
        return null;
    }

    @Override
    public FakeEntity find(Query filter) {
        return null;
    }

    @Override
    public PaginableResult<FakeEntity> findAll(Query filter, int delta, int page, QueryOrder queryOrder) {
        return null;
    }

    @Override
    public long countAll(Query filter) {
        return 0;
    }

    @Override
    public Class<FakeEntity> getEntityType() {
        return FakeEntity.class;
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return null;
    }
}
