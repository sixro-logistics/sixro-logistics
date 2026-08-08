package com.sixro.logistics.hub.domain.model;

import com.sixro.logistics.common.core.exception.BaseException;
import com.sixro.logistics.hub.domain.exception.HubErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Location {

    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(), 4326);

    @Column(name = "longitude", nullable = false)
    private double longitude;

    @Column(name = "latitude", nullable = false)
    private double latitude;

    @Column(name = "location", nullable = false, columnDefinition = "geometry(Point, 4326)")
    private Point point;

    private Location(double longitude, double latitude) {
        validateKoreaBounds(longitude, latitude);

        this.longitude = longitude;
        this.latitude = latitude;
        this.point = GEOMETRY_FACTORY.createPoint(new Coordinate(longitude, latitude));
    }

    private void validateKoreaBounds(double longitude, double latitude) {
        if (longitude < 124 || longitude > 132 || latitude < 33 || latitude > 39) {
            throw new BaseException(HubErrorCode.INVALID_LOCATION_BOUNDS);
        }
    }

    public static Location of(double longitude, double latitude) {
        return new Location(longitude, latitude);
    }
}
