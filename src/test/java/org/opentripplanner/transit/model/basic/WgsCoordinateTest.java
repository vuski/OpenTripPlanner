package org.opentripplanner.transit.model.basic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;

public class WgsCoordinateTest {

  @Test
  public void testToString() {
    WgsCoordinate c = new WgsCoordinate(1.123456789, 2.987654321);
    assertEquals("(1.12346, 2.98765)", c.toString());
    assertEquals("(1.123, 2.9)", new WgsCoordinate(1.123, 2.9).toString());
  }

  @Test
  public void testCoordinateEquals() {
    WgsCoordinate a = new WgsCoordinate(5.0, 3.0);

    // Test latitude
    WgsCoordinate sameLatitude = new WgsCoordinate(5.000_000_099, 3.0);
    WgsCoordinate differentLatitude = new WgsCoordinate(5.000_000_101, 3.0);

    assertTrue(a.sameLocation(sameLatitude));
    assertFalse(a.sameLocation(differentLatitude));

    // Test longitude
    WgsCoordinate sameLongitude = new WgsCoordinate(5.0, 3.000_000_099);
    WgsCoordinate differentLongitude = new WgsCoordinate(5.0, 3.000_000_101);

    assertTrue(a.sameLocation(sameLongitude));
    assertFalse(a.sameLocation(differentLongitude));
  }

  @Test
  public void asJtsCoordinate() {
    // Given a well known location in Oslo
    double latitude = 59.9110583;
    double longitude = 10.7502691;
    WgsCoordinate c = new WgsCoordinate(latitude, longitude);

    // The convert to JTS:
    Coordinate jts = c.asJtsCoordinate();

    // Assert latitude is y, and longitude is x coordinate
    assertEquals(latitude, jts.y, 1E-7);
    assertEquals(longitude, jts.x, 1E-7);
  }

  @Test
  public void mean() {
    var c1 = new WgsCoordinate(10.0, 5.0);
    var c2 = new WgsCoordinate(20.0, -5.0);

    var m = WgsCoordinate.mean(List.of(c1));
    assertSame(c1, m);

    var m1 = WgsCoordinate.mean(List.of(c1, c2));
    assertTrue(new WgsCoordinate(15.0, 0.0).sameLocation(m1));

    assertThrows(IllegalArgumentException.class, () -> WgsCoordinate.mean(List.of()));
  }
}