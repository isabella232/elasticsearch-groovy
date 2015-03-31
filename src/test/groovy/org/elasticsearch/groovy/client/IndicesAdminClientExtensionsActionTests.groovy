/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.elasticsearch.groovy.client

import org.apache.lucene.util.LuceneTestCase.BadApple

import org.elasticsearch.action.admin.indices.mapping.get.GetMappingsResponse
import org.elasticsearch.action.admin.indices.refresh.RefreshResponse
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.client.IndicesAdminClient
import org.elasticsearch.cluster.metadata.MappingMetaData

import org.junit.Before
import org.junit.Test

/**
 * Tests {@code ActionRequest}s added by {@link IndicesAdminClientExtensions}.
 */
class IndicesAdminClientExtensionsActionTests extends AbstractClientTests {
    /**
     * The index to use for most tests.
     */
    String indexName = 'indices'
    /**
     * The index type to use for most tests.
     */
    String typeName = 'actions'

    /**
     * Enhanced {@link IndicesAdminClient} that is tested alongside relevant {@link #client} actions.
     */
    IndicesAdminClient indicesAdminClient

    @Before
    void setupAdmin() {
        indicesAdminClient = client.admin.indices
    }

    // REPLACE WITH non-Async variants in 2.0

    @Test
    void testRefreshRequest() {
        String docId = indexDoc(indexName, typeName) {
            name = "needle"
        }

        // refresh the index to guarantee searchability
        RefreshResponse response = indicesAdminClient.refresh {
            indices indexName
        }.actionGet()

        assert response.failedShards == 0

        // because we have refreshed the index, we should now be able to search for documents guaranteed
        SearchResponse searchResponse = client.search {
            indices indexName
            types typeName
            source {
                query {
                    match {
                        name = "needle"
                    }
                }
            }
        }.actionGet()

        assert searchResponse.hits.totalHits == 1
        assert searchResponse.hits.hits[0].id == docId
    }

    //
    // START OF requestAsync test variants (combine sections with sync versus async variant side-by-side in 2.0)
    //

    @Test
    void testRefreshRequestAsync() {
        String docId = indexDoc(indexName, typeName) {
            name = "needle"
        }

        // refresh the index to guarantee searchability
        RefreshResponse response = indicesAdminClient.refreshAsync {
            indices indexName
        }.actionGet()

        assert response.failedShards == 0

        // because we have refreshed the index, we should now be able to search for documents guaranteed
        SearchResponse searchResponse = client.searchAsync {
            indices indexName
            types typeName
            source {
                query {
                    match {
                        name = "needle"
                    }
                }
            }
        }.actionGet()

        assert searchResponse.hits.totalHits == 1
        assert searchResponse.hits.hits[0].id == docId
    }

    @BadApple(bugUrl = 'Checking to see if the logic is flawed, or if the source is flawed')
    @Test
    void testGetMappingRequest() {
        // index a document to guarantee that a mapping exists
        indexDoc(indexName, typeName) {
            name = "needle"
        }

        // ensure that the mapping exists
        // refresh the index to guarantee searchability
        RefreshResponse refreshResponse = indicesAdminClient.refresh {
            indices indexName
        }.actionGet()

        assert refreshResponse.failedShards == 0

        GetMappingsResponse response = indicesAdminClient.getMappings {
            indices indexName
        }.actionGet()

        // extra the mapping for the current index/type
        MappingMetaData mappingMetaData = response.mappings.get(indexName).get(typeName)

        // ensure that we properly mapped the 'name' field to a string
        assert mappingMetaData.sourceAsMap['properties']['name']['type'] == 'string'
    }
}
