package org.opentripplanner.raptor.api.request;

import org.opentripplanner.raptor.api.model.DominanceFunction;

public class RaptorTransitPassthroughRequest implements C2Request {

  private final PassthroughPoints passthroughPoints;

  public RaptorTransitPassthroughRequest(final PassthroughPoints passthroughPoints) {
    this.passthroughPoints = passthroughPoints;
  }

  // TODO: Should this method be part of the interface?
  public PassthroughPoints passthroughPoints() {
    return passthroughPoints;
  }

  /**
   * This is the dominance function to use for comparing transit-priority-groupIds.
   * It is critical that the implementation is "static" so it can be inlined, since it
   * is run in the innermost loop of Raptor.
   */
  @Override
  public DominanceFunction dominanceFunction() {
    return (left, right) -> left > right;
  }
}
