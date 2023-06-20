package org.opentripplanner.ext.transmodelapi.mapping;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.opentripplanner.transit.model.site.StopLocations;
import org.opentripplanner.transit.service.TransitService;

class PassthroughLocationMapper {

  static List<StopLocations> toLocations(
    final TransitService transitService,
    final List<Map<String, Object>> passthroughPoints
  ) {
    return passthroughPoints
      .stream()
      .map(m ->
        m
          .entrySet()
          .stream()
          .filter(e -> e.getKey().equals("places"))
          .findFirst()
          .map(e -> handlePoint(transitService, (List<String>) e.getValue()))
      )
      .flatMap(Optional::stream)
      .collect(toList());
    // TODO Propagate an error if a stopplace is unknown and fails lookup.
  }

  private static StopLocations handlePoint(
    final TransitService transitService,
    final List<String> stops
  ) {
    return stops
      .stream()
      .map(sp -> TransitIdMapper.mapIDToDomain(sp))
      .map(id -> transitService.getStopLocation(id))
      .collect(collectingAndThen(toList(), sls -> new StopLocations(sls)));
  }
}
