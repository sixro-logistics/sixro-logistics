package com.sixro.logistics.hub.route.application.query;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public record GeoJsonLineString(
        String type,
        List<List<Double>> coordinates
) implements Serializable {
    public static GeoJsonLineString from(LineString lineString) {
        if (lineString == null || lineString.isEmpty()) {
            return null;
        }

        List<List<Double>> coords = new ArrayList<>();
        for (Coordinate coord : lineString.getCoordinates()) {
            coords.add(List.of(coord.getX(), coord.getY()));
        }

        return new GeoJsonLineString("LineString", coords);
    }
}