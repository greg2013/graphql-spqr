package io.leangen.graphql.generator.mapping.common;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLInterfaceType;
import graphql.schema.GraphQLObjectType;
import io.leangen.graphql.annotations.types.GraphQLInterface;
import io.leangen.graphql.generator.BuildContext;
import io.leangen.graphql.generator.OperationMapper;
import io.leangen.graphql.generator.mapping.strategy.InterfaceMappingStrategy;
import io.leangen.graphql.generator.types.MappedGraphQLInterfaceType;

import java.lang.reflect.AnnotatedType;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static graphql.schema.GraphQLInterfaceType.newInterface;

/**
 * @author Bojan Tomic (kaqqao)
 */
public class InterfaceMapper extends CachingMapper<GraphQLInterfaceType, GraphQLInputObjectType> {

    private final InterfaceMappingStrategy interfaceStrategy;
    private final ObjectTypeMapper objectTypeMapper;

    public InterfaceMapper(InterfaceMappingStrategy interfaceStrategy, ObjectTypeMapper objectTypeMapper) {
        this.interfaceStrategy = Objects.requireNonNull(interfaceStrategy);
        this.objectTypeMapper = Objects.requireNonNull(objectTypeMapper);
    }

    @Override
    public GraphQLInterfaceType toGraphQLType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        GraphQLInterfaceType.Builder typeBuilder = newInterface()
                .name(typeName)
                .description(buildContext.typeInfoGenerator.generateTypeDescription(javaType));

        List<GraphQLFieldDefinition> fields = objectTypeMapper.getFields(javaType, buildContext, operationMapper);
        fields.forEach(typeBuilder::field);

        typeBuilder.typeResolver(buildContext.typeResolver);
        GraphQLInterfaceType type = new MappedGraphQLInterfaceType(typeBuilder.build(), javaType);

        registerImplementations(javaType, type, operationMapper, buildContext);
        return type;
    }

    @Override
    public GraphQLInputObjectType toGraphQLInputType(String typeName, AnnotatedType javaType, OperationMapper operationMapper, BuildContext buildContext) {
        return objectTypeMapper.toGraphQLInputType(typeName, javaType, operationMapper, buildContext);
    }
    
    @Override
    public boolean supports(AnnotatedType type) {
        return interfaceStrategy.supports(type);
    }

    @SuppressWarnings("WeakerAccess")
    protected void registerImplementations(AnnotatedType javaType, GraphQLInterfaceType type, OperationMapper operationMapper, BuildContext buildContext) {
        if (javaType.isAnnotationPresent(GraphQLInterface.class)) {
            GraphQLInterface meta = javaType.getAnnotation(GraphQLInterface.class);
            if (meta.implementationAutoDiscovery()) {
                buildContext.implDiscoveryStrategy.findImplementations(javaType, meta.scanPackages(), buildContext).forEach(impl ->
                        getImplementingType(impl, operationMapper, buildContext)
                                .ifPresent(implType -> buildContext.typeRepository.registerDiscoveredCovariantType(type.getName(), impl, implType)));
            }
        }
    }

    private Optional<GraphQLObjectType> getImplementingType(AnnotatedType implType, OperationMapper operationMapper, BuildContext buildContext) {
        return Optional.of(implType)
                .filter(impl -> !interfaceStrategy.supports(impl))
                .map(impl -> operationMapper.toGraphQLType(impl, buildContext))
                .filter(impl -> impl instanceof GraphQLObjectType)
                .map(impl -> (GraphQLObjectType) impl);
    }
}
