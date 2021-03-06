package io.leangen.graphql.extension;

import graphql.schema.GraphQLSchema;
import io.leangen.graphql.generator.BuildContext;

@FunctionalInterface
public interface GraphQLSchemaProcessor {

    GraphQLSchema.Builder process(GraphQLSchema.Builder schemaBuilder, BuildContext buildContext);
}
