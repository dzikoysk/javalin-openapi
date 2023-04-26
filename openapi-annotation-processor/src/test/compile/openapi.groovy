import io.javalin.openapi.experimental.AnnotationProcessorContext
import io.javalin.openapi.experimental.ClassDefinition
import io.javalin.openapi.experimental.EmbeddedTypeProcessorContext
import io.javalin.openapi.experimental.ExperimentalCompileOpenApiConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfiguration
import io.javalin.openapi.experimental.OpenApiAnnotationProcessorConfigurer
import io.javalin.openapi.experimental.SimpleType

import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement

@ExperimentalCompileOpenApiConfiguration
class OpenApiConfiguration implements OpenApiAnnotationProcessorConfigurer {

    @Override
    void configure(OpenApiAnnotationProcessorConfiguration configuration) {
        configuration.validateWithParser = false
        // configuration.debug = false

        // Used by TypeMappersTest
        configuration.simpleTypeMappings['io.javalin.openapi.processor.TypeMappersTest.CustomType'] = new SimpleType("string")

        // Used by UserCasesTest
        configuration.propertyInSchemeFilter = { AnnotationProcessorContext ctx, ClassDefinition type, Element property ->
            TypeElement specificRecord = ctx.forTypeElement('io.javalin.openapi.processor.UserCasesTest.SpecificRecord')
            TypeElement specificRecordBase = ctx.forTypeElement('io.javalin.openapi.processor.UserCasesTest.SpecificRecordBase')

            if (ctx.isAssignable(type.mirror, specificRecord.asType()) && ctx.hasElement(specificRecord, property)) {
                return false // exclude
            }

            if (ctx.isAssignable(type.mirror, specificRecordBase.asType()) && ctx.hasElement(specificRecordBase, property)) {
                return false // exclude
            }

            return true // include
        }

        // Used by CustomTypeMappingsTest
        configuration.insertEmbeddedTypeProcessor({ EmbeddedTypeProcessorContext context ->
            if (context.type.simpleName == 'Optional' && context.type.generics.size() == 1) {
                context.parentContext.typeSchemaGenerator.addType(context.scheme, context.type.generics[0], context.inlineRefs, context.references, false)
                return true
            }

            return false
        })
    }

}