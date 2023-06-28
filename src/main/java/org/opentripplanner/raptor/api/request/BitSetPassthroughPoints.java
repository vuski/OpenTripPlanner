package org.opentripplanner.raptor.api.request;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.BitSet;
import java.util.List;
import org.opentripplanner.transit.model.site.StopLocations;

public class BitSetPassthroughPoints implements PassthroughPoints {

  private final List<BitSet> passthroughPoints;

  private BitSetPassthroughPoints(final List<BitSet> passthroughPoints) {
    this.passthroughPoints = passthroughPoints;
  }

  public static PassthroughPoints create(final List<StopLocations> passthroughStops) {
    if (passthroughStops.isEmpty()) {
      return PassthroughPoints.NO_PASSTHROUGH_POINTS;
    }

    return passthroughStops
      .stream()
      .map(vs -> {
        final BitSet tmpBitSet = new BitSet();
        vs.stream().forEach(sl -> tmpBitSet.set(sl.getIndex()));
        return tmpBitSet;
      })
      .collect(collectingAndThen(toList(), vps -> new BitSetPassthroughPoints(vps)));
  }

  @Override
  public boolean isPassthroughPoint(final int passthroughIndex, final int stop) {
    return passthroughPoints.get(passthroughIndex).get(stop);
  }

  @Override
  public int size() {
    return passthroughPoints.size();
  }
}
