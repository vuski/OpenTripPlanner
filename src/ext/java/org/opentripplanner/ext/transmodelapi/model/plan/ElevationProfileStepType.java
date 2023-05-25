package org.opentripplanner.ext.transmodelapi.model.plan;

import graphql.Scalars;
import graphql.schema.DataFetchingEnvironment;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLTypeReference;
import org.opentripplanner.model.plan.ElevationProfile;

public class ElevationProfileStepType {

  private static final String NAME = "ElevationProfileStep";
  public static final GraphQLTypeReference REF = new GraphQLTypeReference(NAME);

  public static GraphQLObjectType create() {
    return GraphQLObjectType
      .newObject()
      .name(NAME)
      .description(
        "Individual step of an elevation profile."
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("distance")
          .description("The distance from the start of the step, in meters.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> step(env).x())
          .build()
      )
      .field(
        GraphQLFieldDefinition
          .newFieldDefinition()
          .name("elevation")
          .description("The elevation at this distance, in meters.")
          .type(Scalars.GraphQLFloat)
          .dataFetcher(env -> step(env).y())
          .build()
      )
      .build();
  }

  private static ElevationProfile.Step step(DataFetchingEnvironment environment) {
    return environment.getSource();
  }
}
