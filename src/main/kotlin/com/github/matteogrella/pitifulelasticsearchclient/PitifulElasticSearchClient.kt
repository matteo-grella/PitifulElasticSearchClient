/* This is free and unencumbered software released into the public domain. */

package com.github.matteogrella.pitifulelasticsearchclient

import com.beust.klaxon.JsonArray
import com.beust.klaxon.JsonObject
import com.beust.klaxon.Parser
import khttp.get
import khttp.put
import java.lang.RuntimeException
import java.lang.StringBuilder

/**
 * A pitiful implementation of an ElasticSearch client with base search functions and without error handling (!).
 *
 * @param host the host
 * @param port the port
 * @param index the index
 * @param type the type
 */
class PitifulElasticSearchClient(
  private val host: String,
  private val port: Int,
  private val index: String,
  private val type: String
) {

  /**
   * The Put.
   *
   * @param id the id
   * @param obj the object to put
   *
   * @return return 'true' on success
   */
  fun put(id: String, obj: JsonObject): Boolean {

    val res = put("http://$host:$port/$index/$type/$id", json = obj)

    return res.jsonObject.getJSONObject("_shards").getInt("successful") >= 1
  }

  /**
   * The Get.
   *
   * @param id the id
   *
   * @return the object of the given [id]
   */
  fun get(id: String): JsonObject {

    val esResult: String = khttp.get("http://$host:$port/$index/$type/$id/_source").text
    val jsonDocument: JsonObject = Parser().parse(StringBuilder(esResult)) as JsonObject

    jsonDocument["id"] = id

    return jsonDocument
  }

  /**
   * The Delete.
   *
   * @param id the id
   *
   * @return return 'true' on success
   */
  fun delete(id: String): Boolean {

    val res = khttp.delete("http://$host:$port/$index/$type/$id")

    return res.jsonObject.getJSONObject("_shards").getInt("successful") >= 1
  }

  /**
   * The Search.
   *
   * @param query the json query
   * @param size the max number of results (limit 10000)
   *
   * @return the list of objects resulting from the [query]
   */
  fun search(query: JsonObject, size: Int = 10000): List<JsonObject> = try {

    assert(size <= 10000)

    val result: String = get("http://$host:$port/$index/$type/_search?size=$size", json = query).jsonObject
      .getJSONObject("hits")
      .getJSONArray("hits")
      .toString()

    (Parser().parse(StringBuilder(result)) as JsonArray<*>).map { (it as JsonObject).obj("_source")!! }

  } catch (e: RuntimeException) {

    listOf() // TODO
  }

  /**
   * The Search.
   *
   * @param query the json-string query
   * @param size the max number of results (limit 10000)
   *
   * @return the list of objects resulting from the [query]
   */
  fun search(query: String, size: Int = 10000) = this.search(Parser().parse(StringBuilder(query)) as JsonObject, size)
}
