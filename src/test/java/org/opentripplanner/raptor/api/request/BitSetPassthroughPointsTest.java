package org.opentripplanner.raptor.api.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static shadow.org.assertj.core.util.Lists.emptyList;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.model.site.StopLocations;

class BitSetPassthroughPointsTest {

  public static final RegularStop STOP_11 = RegularStop.of(FeedScopedId.parseId("1:1")).build();
  public static final RegularStop STOP_12 = RegularStop.of(FeedScopedId.parseId("1:2")).build();
  public static final RegularStop STOP_13 = RegularStop.of(FeedScopedId.parseId("1:3")).build();
  public static final StopLocations STOPLOCATIONS_1 = new StopLocations(
    List.of(STOP_11, STOP_12, STOP_13)
  );
  public static final RegularStop STOP_21 = RegularStop.of(FeedScopedId.parseId("2:1")).build();
  public static final RegularStop STOP_22 = RegularStop.of(FeedScopedId.parseId("2:2")).build();
  public static final RegularStop STOP_23 = RegularStop.of(FeedScopedId.parseId("2:3")).build();
  public static final StopLocations STOPLOCATIONS_2 = new StopLocations(
    List.of(STOP_21, STOP_22, STOP_23)
  );
  public static final RegularStop STOP_31 = RegularStop.of(FeedScopedId.parseId("3:1")).build();
  private static PassthroughPoints PASSTHROUGH_POINTS = BitSetPassthroughPoints.create(
    List.of(STOPLOCATIONS_1, STOPLOCATIONS_2)
  );
  private static PassthroughPoints EMPTY_PASSTHROUGH_POINTS = BitSetPassthroughPoints.create(
    emptyList()
  );

  @Test
  void passthroughPoint() {
    assertTrue(PASSTHROUGH_POINTS.isPassthroughPoint(0, STOP_11.getIndex()));
  }

  @Test
  void passthroughPoint_secondPoint() {
    assertTrue(PASSTHROUGH_POINTS.isPassthroughPoint(1, STOP_22.getIndex()));
  }

  @Test
  void notAPassthroughPoint() {
    assertFalse(PASSTHROUGH_POINTS.isPassthroughPoint(0, STOP_31.getIndex()));
  }

  @Test
  void notAPassthroughPoint_passthroughPointOnIncorrectPosition() {
    assertFalse(PASSTHROUGH_POINTS.isPassthroughPoint(0, STOP_21.getIndex()));
  }

  @Test
  void notAPassthroughPoint_incorrectPassthroughPointIndex() {
    final IndexOutOfBoundsException indexOutOfBoundsException = assertThrows(
      IndexOutOfBoundsException.class,
      () -> PASSTHROUGH_POINTS.isPassthroughPoint(99, STOP_11.getIndex())
    );
    assertEquals("Index 99 out of bounds for length 2", indexOutOfBoundsException.getMessage());
  }

  @Test
  void notAPassthroughPoint_empty() {
    assertFalse(EMPTY_PASSTHROUGH_POINTS.isPassthroughPoint(0, STOP_11.getIndex()));
  }
}
