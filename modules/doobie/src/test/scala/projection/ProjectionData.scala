// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package projection

import cats.effect.Sync
import cats.implicits._
import doobie.Transactor

import edu.gemini.grackle._, doobie._
import edu.gemini.grackle.Predicate.{Const, Eql, FieldPath, Project}
import edu.gemini.grackle.Query.{Binding, Filter, Select}
import edu.gemini.grackle.QueryCompiler.SelectElaborator
import edu.gemini.grackle.Value.{BooleanValue, ObjectValue}

trait ProjectionMapping[F[_]] extends DoobieMapping[F] {

  val schema =
    Schema(
      """
        type Query {
          level0(filter: Filter): [Level0!]!
          level1(filter: Filter): [Level1!]!
          level2(filter: Filter): [Level2!]!
        }
        type Level0 {
          id: String!
          level1(filter: Filter): [Level1!]!
        }
        type Level1 {
          id: String!
          level2(filter: Filter): [Level2!]!
        }
        type Level2 {
          id: String!
          attr: Boolean
        }
        input Filter {
          attr: Boolean
        }
      """
    ).right.get

  val QueryType = schema.ref("Query")
  val Level0Type = schema.ref("Level0")
  val Level1Type = schema.ref("Level1")
  val Level2Type = schema.ref("Level2")


  import DoobieFieldMapping._

  val typeMappings =
    List(
      ObjectMapping(
        tpe = QueryType,
        fieldMappings =
          List(
            DoobieRoot("level0"),
            DoobieRoot("level1"),
            DoobieRoot("level2")
          )
      ),
      ObjectMapping(
        tpe = Level0Type,
        fieldMappings =
          List(
            DoobieField("id", ColumnRef("level0", "id"), key = true),
            DoobieObject("level1", Subobject(
              List(Join(ColumnRef("level0", "id"), ColumnRef("level1", "level0_id")))
            ))
          )
      ),
      ObjectMapping(
        tpe = Level1Type,
        fieldMappings =
          List(
            DoobieField("id", ColumnRef("level1", "id"), key = true),
            DoobieAttribute[String]("level0_id", ColumnRef("level1", "level0_id")),
            DoobieObject("level2", Subobject(
              List(Join(ColumnRef("level1", "id"), ColumnRef("level2", "level1_id")))
            ))
          )
      ),
      ObjectMapping(
        tpe = Level2Type,
        fieldMappings =
          List(
            DoobieField("id", ColumnRef("level2", "id"), key = true),
            DoobieField("attr", ColumnRef("level2", "attr")),
            DoobieAttribute[String]("level1_id", ColumnRef("level2", "level1_id"))
          )
      )
    )

  object Level0FilterValue {
    def unapply(input: ObjectValue): Option[Predicate] = {
      input.fields match {
        case List(("attr", BooleanValue(attr))) =>
          Some(Project(List("level1", "level2"), Eql(FieldPath(List("attr")), Const(attr))))
        case _ => None
      }
    }
  }

  object Level1FilterValue {
    def unapply(input: ObjectValue): Option[Predicate] = {
      input.fields match {
        case List(("attr", BooleanValue(attr))) =>
          Some(Project(List("level2"), Eql(FieldPath(List("attr")), Const(attr))))
        case _ => None
      }
    }
  }

  object Level2FilterValue {
    def unapply(input: ObjectValue): Option[Predicate] = {
      input.fields match {
        case List(("attr", BooleanValue(attr))) =>
          Some(Eql(FieldPath(List("attr")), Const(attr)))
        case _ => None
      }
    }
  }

  override val selectElaborator: SelectElaborator = new SelectElaborator(Map(
    QueryType -> {
      case Select("level0", List(Binding("filter", filter)), child) =>
        val f = filter match {
          case Level0FilterValue(f) => Filter(f, child)
          case _ => child
        }
        Select("level0", Nil, f).rightIor

      case Select("level1", List(Binding("filter", filter)), child) =>
        val f = filter match {
          case Level1FilterValue(f) => Filter(f, child)
          case _ => child
        }
        Select("level1", Nil, f).rightIor

      case Select("level2", List(Binding("filter", filter)), child) =>
        val f = filter match {
          case Level2FilterValue(f) => Filter(f, child)
          case _ => child
        }
        Select("level2", Nil, f).rightIor

      case other => other.rightIor
    },
    Level0Type -> {
      case Select("level1", List(Binding("filter", filter)), child) =>
        val f = filter match {
          case Level1FilterValue(f) => Filter(f, child)
          case _ => child
        }
        Select("level1", Nil, f).rightIor

      case other => other.rightIor
    },
    Level1Type -> {
      case Select("level2", List(Binding("filter", filter)), child) =>
        val f = filter match {
          case Level2FilterValue(f) => Filter(f, child)
          case _ => child
        }
        Select("level2", Nil, f).rightIor

      case other => other.rightIor
    }
  ))
}

object ProjectionMapping extends DoobieMappingCompanion {
  def mkMapping[F[_]: Sync](transactor: Transactor[F], monitor: DoobieMonitor[F]): ProjectionMapping[F] =
    new DoobieMapping(transactor, monitor) with ProjectionMapping[F]
}
