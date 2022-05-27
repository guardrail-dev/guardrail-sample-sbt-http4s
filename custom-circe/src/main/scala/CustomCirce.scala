package guardrailhacks
import dev.guardrail.generators.scala.circe.CirceProtocolGenerator

import _root_.io.swagger.v3.oas.models.media.{ Discriminator => _, _ }
import cats.Monad
import cats.data.{ NonEmptyList, NonEmptyVector }
import cats.syntax.all._
import scala.meta._
import scala.reflect.runtime.universe.typeTag

import dev.guardrail.core
import dev.guardrail.core.extract.{ DataRedaction, EmptyValueIsNull }
import dev.guardrail.core.implicits._
import dev.guardrail.core.{ DataVisible, EmptyIsEmpty, EmptyIsNull, LiteralRawType, ReifiedRawType, ResolvedType, SupportDefinition, Tracker }
import dev.guardrail.generators.spi.ProtocolGeneratorLoader
import dev.guardrail.generators.scala.{ CirceModelGenerator, ScalaGenerator, ScalaLanguage }
import dev.guardrail.generators.RawParameterName
import dev.guardrail.terms.protocol.PropertyRequirement
import dev.guardrail.terms.protocol._
import dev.guardrail.terms.{ ProtocolTerms, RenderedEnum, RenderedIntEnum, RenderedLongEnum, RenderedStringEnum }
import dev.guardrail.{ SwaggerUtil, Target, UserError }

class CustomCirceLoader extends ProtocolGeneratorLoader {
  type L = ScalaLanguage
  def reified = typeTag[Target[ScalaLanguage]]
  def apply(parameters: Set[String]): Option[ProtocolTerms[ScalaLanguage, Target]] = {
    for {
      () <- parameters.collectFirst { case "custom-circe" => () }
      () = println("Let's get dangerous!")
    } yield CustomCirce.instance
  }
}

object CustomCirce {
  private def lookupTypeName(tpeName: String, concreteTypes: List[PropMeta[ScalaLanguage]])(f: Type => Type): Option[Type] =
    concreteTypes
      .find(_.clsName == tpeName)
      .map(_.tpe)
      .map(f)

  def customTransformProperty(
      clsName: String,
      dtoPackage: List[String],
      supportPackage: List[String],
      concreteTypes: List[PropMeta[ScalaLanguage]]
  )(
      name: String,
      fieldName: String,
      property: Tracker[Schema[_]],
      meta: ResolvedType[ScalaLanguage],
      requirement: PropertyRequirement,
      isCustomType: Boolean,
      defaultValue: Option[scala.meta.Term]
  ): Target[ProtocolParameter[ScalaLanguage]] =
    Target.log.function(s"transformProperty") {
      val fallbackRawType = ReifiedRawType.of(property.downField("type", _.getType()).unwrapTracker, property.downField("format", _.getFormat()).unwrapTracker)
      for {
        _ <- Target.log.debug(s"Args: (${clsName}, ${name}, ...)")

        readOnlyKey = Option(name).filter(_ => property.downField("readOnly", _.getReadOnly()).unwrapTracker.contains(true))
        emptyToNull = property
          .refine { case d: DateSchema => d }(d => EmptyValueIsNull(d))
          .orRefine { case dt: DateTimeSchema => dt }(dt => EmptyValueIsNull(dt))
          .orRefine { case s: StringSchema => s }(s => EmptyValueIsNull(s))
          .toOption
          .flatten
          .getOrElse(EmptyIsEmpty)

        dataRedaction = DataRedaction(property).getOrElse(DataVisible)

        (tpe, classDep, rawType) = meta match {
          case core.Resolved(declType, classDep, _, rawType @ LiteralRawType(Some(rawTypeStr), rawFormat))
              if SwaggerUtil.isFile(rawTypeStr, rawFormat) && !isCustomType =>
            // assume that binary data are represented as a string. allow users to override.
            (t"String", classDep, rawType)
          case core.Resolved(declType, classDep, _, rawType) =>
            (declType, classDep, rawType)
          case core.Deferred(tpeName) =>
            val tpe = concreteTypes.find(_.clsName == tpeName).map(_.tpe).getOrElse {
              println(s"Unable to find definition for ${tpeName}, just inlining")
              Type.Name(tpeName)
            }
            (tpe, Option.empty, fallbackRawType)
          case core.DeferredArray(tpeName, containerTpe) =>
            val concreteType = lookupTypeName(tpeName, concreteTypes)(identity)
            val innerType    = concreteType.getOrElse(Type.Name(tpeName))
            (t"${containerTpe.getOrElse(t"_root_.scala.Vector")}[$innerType]", Option.empty, ReifiedRawType.ofVector(fallbackRawType))
          case core.DeferredMap(tpeName, customTpe) =>
            val concreteType = lookupTypeName(tpeName, concreteTypes)(identity)
            val innerType    = concreteType.getOrElse(Type.Name(tpeName))
            (t"${customTpe.getOrElse(t"_root_.scala.Predef.Map")}[_root_.scala.Predef.String, $innerType]", Option.empty, ReifiedRawType.ofMap(fallbackRawType))
        }
        presence     <- ScalaGenerator().selectTerm(NonEmptyList.ofInitLast(supportPackage, "Presence"))
        presenceType <- ScalaGenerator().selectType(NonEmptyList.ofInitLast(supportPackage, "Presence"))
        (finalDeclType, finalDefaultValue) = requirement match {
          case PropertyRequirement.Required => tpe -> defaultValue
          case PropertyRequirement.Optional | PropertyRequirement.Configured(PropertyRequirement.Optional, PropertyRequirement.Optional) =>
            t"$presenceType[$tpe]" -> defaultValue.map(t => q"$presence.Present($t)").orElse(Some(q"$presence.Absent"))
          case _: PropertyRequirement.OptionalRequirement | _: PropertyRequirement.Configured =>
            t"Option[$tpe]" -> defaultValue.map(t => q"Option($t)").orElse(Some(q"None"))
          case PropertyRequirement.OptionalNullable =>
            t"$presenceType[Option[$tpe]]" -> defaultValue.map(t => q"$presence.Present($t)")
        }
        term = param"${Term.Name(fieldName)}: ${finalDeclType}".copy(default = finalDefaultValue)
        dep  = classDep.filterNot(_.value == clsName) // Filter out our own class name
      } yield ProtocolParameter[ScalaLanguage](
        term,
        tpe,
        RawParameterName(name),
        dep,
        rawType,
        readOnlyKey,
        emptyToNull,
        dataRedaction,
        requirement,
        finalDefaultValue
      )
    }


  val instance = CirceProtocolGenerator(CirceModelGenerator.V012).copy(
    transformProperty = customTransformProperty _
  )

}
