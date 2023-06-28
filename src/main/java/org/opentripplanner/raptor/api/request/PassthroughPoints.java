package org.opentripplanner.raptor.api.request;

/**
 * Provides information for passthrough-points used in the search.
 */
public interface PassthroughPoints {
  /** Implementation that answers negative for all stops. */
  PassthroughPoints NO_PASSTHROUGH_POINTS = new PassthroughPoints() {
    @Override
    public boolean isPassthroughPoint(int passthroughIndex, int stop) {
      return false;
    }

    @Override
    public int size() {
      return 0;
    }
  };

  /**
   * If a certain stop is a passthrough point of a certain position in the trip.
   *
   * @param passthroughIndex the index position of the stop in the list of passthrough points, zero-based
   * @param stop the stop index to check
   * @return boolean true if the stop is a passthrough point on the specific position
   * @throws IndexOutOfBoundsException if the throughIndex position is outside the range
   */
  boolean isPassthroughPoint(int passthroughIndex, int stop);

  /**
   * Get the number of pass-through points indexes in the collection
   */
  int size();
}
