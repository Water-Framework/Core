package it.water.core.testing.utils;

import it.water.core.api.model.PaginableResult;
import it.water.core.api.repository.query.Query;
import it.water.core.api.repository.query.QueryBuilder;
import it.water.core.api.repository.query.QueryOrder;
import it.water.core.interceptors.annotations.FrameworkComponent;

@FrameworkComponent
public class FakeRepository implements TestRepository {
    @Override
    public Class<FakeEntity> getEntityType() {
        return FakeEntity.class;
    }

    @Override
    public FakeEntity persist(FakeEntity entity) {
        return null;
    }

    @Override
    public FakeEntity persist(FakeEntity entity, Runnable executeInTransaction) {
        return null;
    }

    @Override
    public FakeEntity update(FakeEntity entity) {
        return null;
    }

    @Override
    public FakeEntity update(FakeEntity entity, Runnable executeInTransaction) {
        return null;
    }

    @Override
    public void remove(long id) {

    }

    @Override
    public void remove(long id, Runnable executeInTransaction) {

    }

    @Override
    public void remove(FakeEntity entity) {

    }

    @Override
    public void removeAllByIds(Iterable<Long> ids) {

    }

    @Override
    public void removeAll(Iterable<FakeEntity> entities) {

    }

    @Override
    public void removeAll() {

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
    public FakeEntity find(String filterStr) {
        return null;
    }

    @Override
    public PaginableResult<FakeEntity> findAll(int delta, int page, Query filter, QueryOrder queryOrder) {
        return null;
    }

    @Override
    public long countAll(Query filter) {
        return 0;
    }

    @Override
    public QueryBuilder getQueryBuilderInstance() {
        return null;
    }
}
