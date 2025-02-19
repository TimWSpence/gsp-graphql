// Copyright (c) 2016-2020 Association of Universities for Research in Astronomy, Inc. (AURA)
// For license information see LICENSE or https://opensource.org/licenses/BSD-3-Clause

package grackle.test

import edu.gemini.grackle.syntax._
import org.scalatest.funsuite.AnyFunSuite
import edu.gemini.grackle.QueryExecutor
import cats.effect.IO
import io.circe.Json

trait SqlWorldSpec extends AnyFunSuite {

  def mapping: QueryExecutor[IO, Json]

  test("simple query") {
    val query = """
      query {
        countries {
          name
        }
      }
    """

    val expected = 239

    val res = mapping.compileAndRun(query).unsafeRunSync()
    // println(res)

    val resSize =
      res
        .hcursor
        .downField("data")
        .downField("countries")
        .values.map(_.size)

    assert(resSize == Some(expected))
  }

  test("simple restricted query") {
    val query = """
      query {
        country(code: "GBR") {
          name
        }
      }
    """

    val expected = json"""
      {
        "data": {
          "country": {
            "name": "United Kingdom"
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("simple restricted nested query") {
    val query = """
      query {
        cities(namePattern: "Ame%") {
          name
          country {
            name
            languages {
              language
            }
          }
        }
      }
    """

    val expected = json"""
      {
        "data": {
          "cities": [
            {
              "name": "Amersfoort",
              "country": {
                "name": "Netherlands",
                "languages": [
                  {
                    "language": "Arabic"
                  },
                  {
                    "language": "Dutch"
                  },
                  {
                    "language": "Fries"
                  },
                  {
                    "language": "Turkish"
                  }
                ]
              }
            },
            {
              "name": "Americana",
              "country": {
                "name": "Brazil",
                "languages": [
                  {
                    "language": "German"
                  },
                  {
                    "language": "Indian Languages"
                  },
                  {
                    "language": "Italian"
                  },
                  {
                    "language": "Japanese"
                  },
                  {
                    "language": "Portuguese"
                  }
                ]
              }
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("multiple aliased root queries") {
    val query = """
      query {
        gbr: country(code: "GBR") {
          name
        }
        fra: country(code: "FRA") {
          name
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "gbr" : {
            "name" : "United Kingdom"
          },
          "fra" : {
            "name" : "France"
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (0)") {
    val query = """
      query {
        cities(namePattern: "Monte-Carlo") {
          name
          country {
            name
            cities {
              name
            }
          }
        }
      }
    """

    val expected = json"""
    {
      "data" : {
        "cities" : [
          {
            "name" : "Monte-Carlo",
            "country" : {
              "name" : "Monaco",
              "cities" : [
                {
                  "name" : "Monte-Carlo"
                },
                {
                  "name" : "Monaco-Ville"
                }
              ]
            }
          }
        ]
      }
    }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (1)") {
    val query = """
      query {
        cities(namePattern: "Monte-Carlo") {
          name
          country {
            name
            cities {
              name
              country {
                name
                cities {
                  name
                  country {
                    name
                  }
                }
              }
            }
          }
        }
      }
    """

    val expected = json"""
    {
      "data" : {
        "cities" : [
          {
            "name" : "Monte-Carlo",
            "country" : {
              "name" : "Monaco",
              "cities" : [
                {
                  "name" : "Monte-Carlo",
                  "country" : {
                    "name" : "Monaco",
                    "cities" : [
                      {
                        "name" : "Monte-Carlo",
                        "country" : {
                          "name" : "Monaco"
                        }
                      },
                      {
                        "name" : "Monaco-Ville",
                        "country" : {
                          "name" : "Monaco"
                        }
                      }
                    ]
                  }
                },
                {
                  "name" : "Monaco-Ville",
                  "country" : {
                    "name" : "Monaco",
                    "cities" : [
                      {
                        "name" : "Monte-Carlo",
                        "country" : {
                          "name" : "Monaco"
                        }
                      },
                      {
                        "name" : "Monaco-Ville",
                        "country" : {
                          "name" : "Monaco"
                        }
                      }
                    ]
                  }
                }
              ]
            }
          }
        ]
      }
    }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (2)") {
    val query = """
      query {
        country(code: "ESP") {
          name
          languages {
            language
            countries {
              name
            }
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "country" : {
            "name" : "Spain",
            "languages" : [
              {
                "language" : "Basque",
                "countries" : [
                  {
                    "name" : "Spain"
                  }
                ]
              },
              {
                "language" : "Catalan",
                "countries" : [
                  {
                    "name" : "Andorra"
                  },
                  {
                    "name" : "Spain"
                  }
                ]
              },
              {
                "language" : "Galecian",
                "countries" : [
                  {
                    "name" : "Spain"
                  }
                ]
              },
              {
                "language" : "Spanish",
                "countries" : [
                  {
                    "name" : "Aruba"
                  },
                  {
                    "name" : "Andorra"
                  },
                  {
                    "name" : "Argentina"
                  },
                  {
                    "name" : "Belize"
                  },
                  {
                    "name" : "Bolivia"
                  },
                  {
                    "name" : "Canada"
                  },
                  {
                    "name" : "Chile"
                  },
                  {
                    "name" : "Colombia"
                  },
                  {
                    "name" : "Costa Rica"
                  },
                  {
                    "name" : "Cuba"
                  },
                  {
                    "name" : "Dominican Republic"
                  },
                  {
                    "name" : "Ecuador"
                  },
                  {
                    "name" : "Spain"
                  },
                  {
                    "name" : "France"
                  },
                  {
                    "name" : "Guatemala"
                  },
                  {
                    "name" : "Honduras"
                  },
                  {
                    "name" : "Mexico"
                  },
                  {
                    "name" : "Nicaragua"
                  },
                  {
                    "name" : "Panama"
                  },
                  {
                    "name" : "Peru"
                  },
                  {
                    "name" : "Puerto Rico"
                  },
                  {
                    "name" : "Paraguay"
                  },
                  {
                    "name" : "El Salvador"
                  },
                  {
                    "name" : "Sweden"
                  },
                  {
                    "name" : "Uruguay"
                  },
                  {
                    "name" : "United States"
                  },
                  {
                    "name" : "Venezuela"
                  },
                  {
                    "name" : "Virgin Islands, U.S."
                  }
                ]
              }
            ]
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (3)") {
    val query = """
      query {
        cities(namePattern: "Lausanne") {
          name
          country {
            name
            cities {
              name
            }
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "cities" : [
            {
              "name" : "Lausanne",
              "country" : {
                "name" : "Switzerland",
                "cities" : [
                  {
                    "name" : "Zürich"
                  },
                  {
                    "name" : "Geneve"
                  },
                  {
                    "name" : "Basel"
                  },
                  {
                    "name" : "Bern"
                  },
                  {
                    "name" : "Lausanne"
                  }
                ]
              }
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (4)") {
    val query = """
      query {
        country(code: "CHE") {
          name
          cities {
            name
            country {
              name
            }
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "country" : {
            "name" : "Switzerland",
            "cities" : [
              {
                "name" : "Zürich",
                "country" : {
                  "name" : "Switzerland"
                }
              },
              {
                "name" : "Geneve",
                "country" : {
                  "name" : "Switzerland"
                }
              },
              {
                "name" : "Basel",
                "country" : {
                  "name" : "Switzerland"
                }
              },
              {
                "name" : "Bern",
                "country" : {
                  "name" : "Switzerland"
                }
              },
              {
                "name" : "Lausanne",
                "country" : {
                  "name" : "Switzerland"
                }
              }
            ]
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (5)") {
    val query = """
      query {
        language(language: "Estonian") {
          language
          countries {
            name
            languages {
              language
            }
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "language" : {
            "language" : "Estonian",
            "countries" : [
              {
                "name" : "Estonia",
                "languages" : [
                  {
                    "language" : "Belorussian"
                  },
                  {
                    "language" : "Estonian"
                  },
                  {
                    "language" : "Finnish"
                  },
                  {
                    "language" : "Russian"
                  },
                  {
                    "language" : "Ukrainian"
                  }
                ]
              },
              {
                "name" : "Finland",
                "languages" : [
                  {
                    "language" : "Estonian"
                  },
                  {
                    "language" : "Finnish"
                  },
                  {
                    "language" : "Russian"
                  },
                  {
                    "language" : "Saame"
                  },
                  {
                    "language" : "Swedish"
                  }
                ]
              }
            ]
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (6)") {
    val query = """
      query {
        cities(namePattern: "Monte-Carlo") {
          name
          a: country {
            name
            b: cities {
              name
              c: country {
                name
                d: cities {
                  name
                  e: country {
                    name
                  }
                }
              }
            }
          }
        }
      }
    """

    val expected = json"""
    {
      "data" : {
        "cities" : [
          {
            "name" : "Monte-Carlo",
            "a" : {
              "name" : "Monaco",
              "b" : [
                {
                  "name" : "Monte-Carlo",
                  "c" : {
                    "name" : "Monaco",
                    "d" : [
                      {
                        "name" : "Monte-Carlo",
                        "e" : {
                          "name" : "Monaco"
                        }
                      },
                      {
                        "name" : "Monaco-Ville",
                        "e" : {
                          "name" : "Monaco"
                        }
                      }
                    ]
                  }
                },
                {
                  "name" : "Monaco-Ville",
                  "c" : {
                    "name" : "Monaco",
                    "d" : [
                      {
                        "name" : "Monte-Carlo",
                        "e" : {
                          "name" : "Monaco"
                        }
                      },
                      {
                        "name" : "Monaco-Ville",
                        "e" : {
                          "name" : "Monaco"
                        }
                      }
                    ]
                  }
                }
              ]
            }
          }
        ]
      }
    }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("recursive query (7)") {
    val query = """
      query {
        cities(namePattern: "Monte-Carlo") {
          name
          country {
            cities {
              name
            }
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "cities" : [
            {
              "name" : "Monte-Carlo",
              "country" : {
                "cities" : [
                  {
                    "name" : "Monte-Carlo"
                  },
                  {
                    "name" : "Monaco-Ville"
                  }
                ]
              }
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("country with no cities") {
    val query = """
      query {
        country(code: "ATA") {
          name
          cities {
            name
          }
        }
      }
    """

    val expected = json"""
      {
        "data": {
          "country": {
            "name": "Antarctica",
            "cities": []
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  // Outer join in which some parents have children and others do not.
  test("countries, some with no cities") {
    val query = """
      query {
        countries {
          name
          cities {
            name
          }
        }
      }
    """
    val json      = mapping.compileAndRun(query).unsafeRunSync()

    val countries = //root.data.countries.arr.getOption(json).get
      json
        .hcursor
        .downField("data")
        .downField("countries")
        .values
        .map(_.toVector)
        .get

    val map = countries.map(j => j.hcursor.downField("name").as[String].toOption.get -> j.hcursor.downField("cities").values.map(_.size).get).toMap

    assert(map("Kazakstan")  == 21)
    assert(map("Antarctica") == 0)
  }

  test("no such country") {
    val query = """
      query {
        country(code: "XXX") {
          name
          cities {
            name
          }
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "country" : null
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("multiple missing countries") {
    val query = """
      query {
        xxx: country(code: "xxx") {
          name
        }
        yyy: country(code: "yyy") {
          name
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "xxx" : null,
          "yyy" : null
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("nullable column (null)") {
    val query = """
    query {
        country(code: "ANT") {
          name
          indepyear
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "country" : {
            "name" : "Netherlands Antilles",
            "indepyear" : null
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("nullable column (non-null)") {
    val query = """
    query {
        country(code: "USA") {
          name
          indepyear
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "country" : {
            "name" : "United States",
            "indepyear" : 1776
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("query with introspection") {
    val query = """
      query {
        country(code: "GBR") {
          __typename
          name
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "country" : {
            "__typename" : "Country",
            "name" : "United Kingdom"
          }
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("structured predicates") {
    val query = """
      query {
        search(minPopulation: 20000000, indepSince: 1980) {
          name
          population
          indepyear
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "search" : [
            {
              "name" : "Russian Federation",
              "population" : 146934000,
              "indepyear" : 1991
            },
            {
              "name" : "Ukraine",
              "population" : 50456000,
              "indepyear" : 1991
            },
            {
              "name" : "Uzbekistan",
              "population" : 24318000,
              "indepyear" : 1991
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("simple query with limit") {
    val query = """
      query {
        countries(limit: 3) {
          name
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "countries" : [
            {
              "name" : "Aruba"
            },
            {
              "name" : "Afghanistan"
            },
            {
              "name" : "Angola"
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("simple query with limit, filter and ordering") {
    val query = """
      query {
        countries(limit: 3, minPopulation: 1, byPopulation: true) {
          name
          population
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "countries" : [
            {
              "name" : "Pitcairn",
              "population" : 50
            },
            {
              "name" : "Cocos (Keeling) Islands",
              "population" : 600
            },
            {
              "name" : "Holy See (Vatican City State)",
              "population" : 1000
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }

  test("query with ordering, ordering field not selected") {
    val query = """
      query {
        countries(limit: 10, byPopulation: true) {
          name
        }
      }
    """

    val expected = json"""
      {
        "data" : {
          "countries" : [
            {
              "name" : "Antarctica"
            },
            {
              "name" : "French Southern territories"
            },
            {
              "name" : "Bouvet Island"
            },
            {
              "name" : "Heard Island and McDonald Islands"
            },
            {
              "name" : "British Indian Ocean Territory"
            },
            {
              "name" : "South Georgia and the South Sandwich Islands"
            },
            {
              "name" : "United States Minor Outlying Islands"
            },
            {
              "name" : "Pitcairn"
            },
            {
              "name" : "Cocos (Keeling) Islands"
            },
            {
              "name" : "Holy See (Vatican City State)"
            }
          ]
        }
      }
    """

    val res = mapping.compileAndRun(query).unsafeRunSync()
    //println(res)

    assert(res == expected)
  }
}
