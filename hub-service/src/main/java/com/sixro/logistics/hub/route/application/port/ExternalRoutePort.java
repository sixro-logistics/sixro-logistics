package com.sixro.logistics.hub.route.application.port;

public interface ExternalRoutePort {

    RouteSnapshotResult getRouteSnapshot(RouteSearchCondition condition);
}