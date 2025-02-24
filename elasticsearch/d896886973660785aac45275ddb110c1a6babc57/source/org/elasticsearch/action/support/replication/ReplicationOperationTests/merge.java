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
package org.elasticsearch.action.support.replication;

import org.apache.lucene.index.CorruptIndexException;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.UnavailableShardsException;
import org.elasticsearch.action.WriteConsistencyLevel;
import org.elasticsearch.action.support.PlainActionFuture;
import org.elasticsearch.action.support.replication.ReplicationResponse.ShardInfo;
import org.elasticsearch.cluster.ClusterState;
import org.elasticsearch.cluster.node.DiscoveryNodes;
import org.elasticsearch.cluster.routing.IndexShardRoutingTable;
import org.elasticsearch.cluster.routing.ShardRouting;
import org.elasticsearch.cluster.routing.ShardRoutingState;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.logging.ESLogger;
import org.elasticsearch.common.util.concurrent.ConcurrentCollections;
import org.elasticsearch.index.shard.IndexShardNotStartedException;
import org.elasticsearch.index.shard.IndexShardState;
import org.elasticsearch.index.shard.ShardId;
import org.elasticsearch.test.ESTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.elasticsearch.action.support.replication.ClusterStateCreationUtils.state;
import static org.elasticsearch.action.support.replication.ClusterStateCreationUtils.stateWithActivePrimary;
import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class ReplicationOperationTests extends ESTestCase {

    public void testReplication() throws Exception {
        final String index = "test";
        final ShardId shardId = new ShardId(index, "_na_", 0);

        ClusterState state = stateWithActivePrimary(index, true, randomInt(5));
        final long primaryTerm = state.getMetaData().index(index).primaryTerm(0);
        final IndexShardRoutingTable indexShardRoutingTable = state.getRoutingTable().shardRoutingTable(shardId);
        ShardRouting primaryShard = indexShardRoutingTable.primaryShard();
        if (primaryShard.relocating() && randomBoolean()) {
            // simulate execution of the replication phase on the relocation target node after relocation source was marked as relocated
            state = ClusterState.builder(state)
                .nodes(DiscoveryNodes.builder(state.nodes()).localNodeId(primaryShard.relocatingNodeId())).build();
            primaryShard = primaryShard.buildTargetRelocatingShard();
        }

        final Set<ShardRouting> expectedReplicas = getExpectedReplicas(shardId, state);

        final Map<ShardRouting, Throwable> expectedFailures = new HashMap<>();
        final Set<ShardRouting> expectedFailedShards = new HashSet<>();
        for (ShardRouting replica : expectedReplicas) {
            if (randomBoolean()) {
                Throwable t;
                boolean criticalFailure = randomBoolean();
                if (criticalFailure) {
                    t = new CorruptIndexException("simulated", (String) null);
                } else {
                    t = new IndexShardNotStartedException(shardId, IndexShardState.RECOVERING);
                }
                logger.debug("--> simulating failure on {} with [{}]", replica, t.getClass().getSimpleName());
                expectedFailures.put(replica, t);
                if (criticalFailure) {
                    expectedFailedShards.add(replica);
                }
            }
        }

        Request request = new Request(shardId);
        PlainActionFuture<TestPrimary.Result> listener = new PlainActionFuture<>();
        final ClusterState finalState = state;
        final TestReplicaProxy replicasProxy = new TestReplicaProxy(expectedFailures);
        final TestPrimary primary = new TestPrimary(primaryShard, primaryTerm);
        final TestReplicationOperation op = new TestReplicationOperation(request,
            primary, listener, replicasProxy, () -> finalState);
        op.execute();

        assertThat(request.primaryTerm(), equalTo(primaryTerm));
        assertThat("request was not processed on primary", request.processedOnPrimary.get(), equalTo(true));
        assertThat(request.processedOnReplicas, equalTo(expectedReplicas));
        assertThat(replicasProxy.failedReplicas, equalTo(expectedFailedShards));
        assertTrue("listener is not marked as done", listener.isDone());
        ShardInfo shardInfo = listener.actionGet().getShardInfo();
        assertThat(shardInfo.getFailed(), equalTo(expectedFailedShards.size()));
        assertThat(shardInfo.getFailures(), arrayWithSize(expectedFailedShards.size()));
        assertThat(shardInfo.getSuccessful(), equalTo(1 + expectedReplicas.size() - expectedFailures.size()));
        final List<ShardRouting> unassignedShards =
            indexShardRoutingTable.shardsWithState(ShardRoutingState.UNASSIGNED);
        final int totalShards = 1 + expectedReplicas.size() + unassignedShards.size();
        assertThat(shardInfo.getTotal(), equalTo(totalShards));

        assertThat(primary.knownLocalCheckpoints.remove(primaryShard.allocationId().getId()), equalTo(primary.localCheckpoint));
        assertThat(primary.knownLocalCheckpoints, equalTo(replicasProxy.generatedLocalCheckpoints));
    }


    public void testReplicationWithShadowIndex() throws Exception {
        final String index = "test";
        final ShardId shardId = new ShardId(index, "_na_", 0);

        final ClusterState state = stateWithActivePrimary(index, true, randomInt(5));
        final long primaryTerm = state.getMetaData().index(index).primaryTerm(0);
        final IndexShardRoutingTable indexShardRoutingTable = state.getRoutingTable().shardRoutingTable(shardId);
        final ShardRouting primaryShard = indexShardRoutingTable.primaryShard();

        Request request = new Request(shardId);
        PlainActionFuture<TestPrimary.Result> listener = new PlainActionFuture<>();
        final TestReplicationOperation op = new TestReplicationOperation(request,
            new TestPrimary(primaryShard, primaryTerm), listener, false, false,
            new TestReplicaProxy(), () -> state, logger, "test");
        op.execute();
        assertThat("request was not processed on primary", request.processedOnPrimary.get(), equalTo(true));
        assertThat(request.processedOnReplicas, equalTo(Collections.emptySet()));
        assertTrue("listener is not marked as done", listener.isDone());
        ShardInfo shardInfo = listener.actionGet().getShardInfo();
        assertThat(shardInfo.getFailed(), equalTo(0));
        assertThat(shardInfo.getFailures(), arrayWithSize(0));
        assertThat(shardInfo.getSuccessful(), equalTo(1));
        assertThat(shardInfo.getTotal(), equalTo(indexShardRoutingTable.getSize()));
    }


    public void testDemotedPrimary() throws Exception {
        final String index = "test";
        final ShardId shardId = new ShardId(index, "_na_", 0);

        ClusterState state = stateWithActivePrimary(index, true, 1 + randomInt(2), randomInt(2));
        final long primaryTerm = state.getMetaData().index(index).primaryTerm(0);
        ShardRouting primaryShard = state.getRoutingTable().shardRoutingTable(shardId).primaryShard();
        if (primaryShard.relocating() && randomBoolean()) {
            // simulate execution of the replication phase on the relocation target node after relocation source was marked as relocated
            state = ClusterState.builder(state)
                .nodes(DiscoveryNodes.builder(state.nodes()).localNodeId(primaryShard.relocatingNodeId())).build();
            primaryShard = primaryShard.buildTargetRelocatingShard();
        }

        final Set<ShardRouting> expectedReplicas = getExpectedReplicas(shardId, state);

        final Map<ShardRouting, Throwable> expectedFailures = new HashMap<>();
        final ShardRouting failedReplica = randomFrom(new ArrayList<>(expectedReplicas));
        expectedFailures.put(failedReplica, new CorruptIndexException("simulated", (String) null));

        Request request = new Request(shardId);
        PlainActionFuture<TestPrimary.Result> listener = new PlainActionFuture<>();
        final ClusterState finalState = state;
        final TestReplicaProxy replicasProxy = new TestReplicaProxy(expectedFailures) {
            @Override
            public void failShard(ShardRouting replica, ShardRouting primary, String message, Throwable throwable,
                                  Runnable onSuccess, Consumer<Throwable> onPrimaryDemoted,
                                  Consumer<Throwable> onIgnoredFailure) {
                assertThat(replica, equalTo(failedReplica));
                onPrimaryDemoted.accept(new ElasticsearchException("the king is dead"));
            }
        };
        AtomicBoolean primaryFailed = new AtomicBoolean();
        final TestPrimary primary = new TestPrimary(primaryShard, primaryTerm) {
            @Override
            public void failShard(String message, Throwable throwable) {
                assertTrue(primaryFailed.compareAndSet(false, true));
            }
        };
        final TestReplicationOperation op = new TestReplicationOperation(request, primary, listener, replicasProxy,
            () -> finalState);
        op.execute();

        assertThat("request was not processed on primary", request.processedOnPrimary.get(), equalTo(true));
        assertTrue("listener is not marked as done", listener.isDone());
        assertTrue(primaryFailed.get());
        assertListenerThrows("should throw exception to trigger retry", listener,
            ReplicationOperation.RetryOnPrimaryException.class);
    }

    public void testAddedReplicaAfterPrimaryOperation() throws Exception {
        final String index = "test";
        final ShardId shardId = new ShardId(index, "_na_", 0);
        final ClusterState initialState = stateWithActivePrimary(index, true, 0);
        final ClusterState stateWithAddedReplicas;
        if (randomBoolean()) {
            stateWithAddedReplicas = state(index, true, ShardRoutingState.STARTED,
                randomBoolean() ? ShardRoutingState.INITIALIZING : ShardRoutingState.STARTED);
        } else {
            stateWithAddedReplicas = state(index, true, ShardRoutingState.RELOCATING);
        }
        testClusterStateChangeAfterPrimaryOperation(shardId, initialState, stateWithAddedReplicas);
    }

    public void testIndexDeletedAfterPrimaryOperation() throws Exception {
        final String index = "test";
        final ShardId shardId = new ShardId(index, "_na_", 0);
        final ClusterState initialState = state(index, true, ShardRoutingState.STARTED, ShardRoutingState.STARTED);
        final ClusterState stateWithDeletedIndex = state(index + "_new", true, ShardRoutingState.STARTED, ShardRoutingState.RELOCATING);
        testClusterStateChangeAfterPrimaryOperation(shardId, initialState, stateWithDeletedIndex);
    }


    private void testClusterStateChangeAfterPrimaryOperation(final ShardId shardId,
                                                             final ClusterState initialState,
                                                             final ClusterState changedState) throws Exception {
        AtomicReference<ClusterState> state = new AtomicReference<>(initialState);
        logger.debug("--> using initial state:\n{}", state.get().prettyPrint());
        final long primaryTerm = initialState.getMetaData().index(shardId.getIndexName()).primaryTerm(shardId.id());
        final ShardRouting primaryShard = state.get().routingTable().shardRoutingTable(shardId).primaryShard();
        final TestPrimary primary = new TestPrimary(primaryShard, primaryTerm) {
            @Override
            public Result perform(Request request) throws Exception {
                Result result = super.perform(request);
                state.set(changedState);
                logger.debug("--> state after primary operation:\n{}", state.get().prettyPrint());
                return result;
            }
        };

        Request request = new Request(shardId);
        PlainActionFuture<TestPrimary.Result> listener = new PlainActionFuture<>();
        final TestReplicationOperation op = new TestReplicationOperation(request, primary, listener,
            new TestReplicaProxy(), state::get);
        op.execute();

        assertThat("request was not processed on primary", request.processedOnPrimary.get(), equalTo(true));
        Set<ShardRouting> expectedReplicas = getExpectedReplicas(shardId, state.get());
        assertThat(request.processedOnReplicas, equalTo(expectedReplicas));
    }

    public void testWriteConsistency() throws Exception {
        final String index = "test";
        final ShardId shardId = new ShardId(index, "_na_", 0);
        final int assignedReplicas = randomInt(2);
        final int unassignedReplicas = randomInt(2);
        final int totalShards = 1 + assignedReplicas + unassignedReplicas;
        final boolean passesWriteConsistency;
        Request request = new Request(shardId).consistencyLevel(randomFrom(WriteConsistencyLevel.values()));
        switch (request.consistencyLevel()) {
            case ONE:
                passesWriteConsistency = true;
                break;
            case DEFAULT:
            case QUORUM:
                if (totalShards <= 2) {
                    passesWriteConsistency = true; // primary is enough
                } else {
                    passesWriteConsistency = assignedReplicas + 1 >= (totalShards / 2) + 1;
                }
                // we have to reset default (as the transport replication action will do)
                request.consistencyLevel(WriteConsistencyLevel.QUORUM);
                break;
            case ALL:
                passesWriteConsistency = unassignedReplicas == 0;
                break;
            default:
                throw new RuntimeException("unknown consistency level [" + request.consistencyLevel() + "]");
        }
        ShardRoutingState[] replicaStates = new ShardRoutingState[assignedReplicas + unassignedReplicas];
        for (int i = 0; i < assignedReplicas; i++) {
            replicaStates[i] = randomFrom(ShardRoutingState.STARTED, ShardRoutingState.RELOCATING);
        }
        for (int i = assignedReplicas; i < replicaStates.length; i++) {
            replicaStates[i] = ShardRoutingState.UNASSIGNED;
        }

        final ClusterState state = state(index, true, ShardRoutingState.STARTED, replicaStates);
        logger.debug("using consistency level of [{}], assigned shards [{}], total shards [{}]." +
                " expecting op to [{}]. using state: \n{}",
            request.consistencyLevel(), 1 + assignedReplicas, 1 + assignedReplicas + unassignedReplicas,
            passesWriteConsistency ? "succeed" : "retry",
            state.prettyPrint());
        final long primaryTerm = state.metaData().index(index).primaryTerm(shardId.id());
        final IndexShardRoutingTable shardRoutingTable = state.routingTable().index(index).shard(shardId.id());
        PlainActionFuture<TestPrimary.Result> listener = new PlainActionFuture<>();
        final ShardRouting primaryShard = shardRoutingTable.primaryShard();
        final TestReplicationOperation op = new TestReplicationOperation(request,
            new TestPrimary(primaryShard, primaryTerm),
            listener, randomBoolean(), true, new TestReplicaProxy(), () -> state, logger, "test");

        if (passesWriteConsistency) {
            assertThat(op.checkWriteConsistency(), nullValue());
            op.execute();
            assertTrue("operations should have been performed, consistency level is met",
                request.processedOnPrimary.get());
        } else {
            assertThat(op.checkWriteConsistency(), notNullValue());
            op.execute();
            assertFalse("operations should not have been perform, consistency level is *NOT* met",
                request.processedOnPrimary.get());
            assertListenerThrows("should throw exception to trigger retry", listener, UnavailableShardsException.class);
        }
    }

    private Set<ShardRouting> getExpectedReplicas(ShardId shardId, ClusterState state) {
        Set<ShardRouting> expectedReplicas = new HashSet<>();
        String localNodeId = state.nodes().getLocalNodeId();
        if (state.routingTable().hasIndex(shardId.getIndexName())) {
            for (ShardRouting shardRouting : state.routingTable().shardRoutingTable(shardId)) {
                if (shardRouting.unassigned()) {
                    continue;
                }
                if (localNodeId.equals(shardRouting.currentNodeId()) == false) {
                    expectedReplicas.add(shardRouting);
                }

                if (shardRouting.relocating() && localNodeId.equals(shardRouting.relocatingNodeId()) == false) {
                    expectedReplicas.add(shardRouting.buildTargetRelocatingShard());
                }
            }
        }
        return expectedReplicas;
    }


    public static class Request extends ReplicationRequest<Request> {
        public AtomicBoolean processedOnPrimary = new AtomicBoolean();
        public Set<ShardRouting> processedOnReplicas = ConcurrentCollections.newConcurrentSet();

        public Request() {
        }

        Request(ShardId shardId) {
            this();
            this.shardId = shardId;
            this.index = shardId.getIndexName();
            // keep things simple
        }

        @Override
        public void writeTo(StreamOutput out) throws IOException {
            super.writeTo(out);
        }

        @Override
        public void readFrom(StreamInput in) throws IOException {
            super.readFrom(in);
        }
    }

    static class TestPrimary implements ReplicationOperation.Primary<Request, Request, TestPrimary.Result> {
        final ShardRouting routing;
        final long term;
        final long localCheckpoint;
        final Map<String, Long> knownLocalCheckpoints = new HashMap<>();

        TestPrimary(ShardRouting routing, long term) {
            this.routing = routing;
            this.term = term;
            this.localCheckpoint = random().nextLong();
        }

        @Override
        public ShardRouting routingEntry() {
            return routing;
        }

        @Override
        public void failShard(String message, Throwable throwable) {
            throw new AssertionError("should shouldn't be failed with [" + message + "]", throwable);
        }

        @Override
        public Result perform(Request request) throws Exception {
            if (request.processedOnPrimary.compareAndSet(false, true) == false) {
                fail("processed [" + request + "] twice");
            }
            request.primaryTerm(term);
            return new Result(request);
        }

        static class Result implements ReplicationOperation.PrimaryResult<Request> {
            private final Request request;
            private ShardInfo shardInfo;

            public Result(Request request) {
                this.request = request;
            }

            @Override
            public Request replicaRequest() {
                return request;
            }

            @Override
            public void setShardInfo(ShardInfo shardInfo) {
                this.shardInfo = shardInfo;
            }

            public ShardInfo getShardInfo() {
                return shardInfo;
            }
        }

        @Override
        public void updateLocalCheckpointForShard(String allocationId, long checkpoint) {
            knownLocalCheckpoints.put(allocationId, checkpoint);
        }

        @Override
        public long localCheckpoint() {
            return localCheckpoint;
        }
    }

    static class ReplicaResponse implements ReplicationOperation.ReplicaResponse {
        final String allocationId;
        final long localCheckpoint;

        ReplicaResponse(String allocationId, long localCheckpoint) {
            this.allocationId = allocationId;
            this.localCheckpoint = localCheckpoint;
        }

        @Override
        public long localCheckpoint() {
            return localCheckpoint;
        }

        @Override
        public String allocationId() {
            return allocationId;
        }
    }

    static class TestReplicaProxy implements ReplicationOperation.Replicas<Request> {

        final Map<ShardRouting, Throwable> opFailures;

        final Set<ShardRouting> failedReplicas = ConcurrentCollections.newConcurrentSet();

        final Map<String, Long> generatedLocalCheckpoints = ConcurrentCollections.newConcurrentMap();

        TestReplicaProxy() {
            this(Collections.emptyMap());
        }

        TestReplicaProxy(Map<ShardRouting, Throwable> opFailures) {
            this.opFailures = opFailures;
        }

        @Override
        public void performOn(ShardRouting replica, Request request, ActionListener<ReplicationOperation.ReplicaResponse> listener) {
            assertTrue("replica request processed twice on [" + replica + "]", request.processedOnReplicas.add(replica));
            if (opFailures.containsKey(replica)) {
                listener.onFailure(opFailures.get(replica));
            } else {
                final long checkpoint = random().nextLong();
                final String allocationId = replica.allocationId().getId();
                Long existing = generatedLocalCheckpoints.put(allocationId, checkpoint);
                assertNull(existing);
                listener.onResponse(new ReplicaResponse(allocationId, checkpoint));
            }
        }

        @Override
        public void failShard(ShardRouting replica, ShardRouting primary, String message, Throwable throwable, Runnable onSuccess,
                              Consumer<Throwable> onPrimaryDemoted, Consumer<Throwable> onIgnoredFailure) {
            if (failedReplicas.add(replica) == false) {
                fail("replica [" + replica + "] was failed twice");
            }
            if (opFailures.containsKey(replica)) {
                if (randomBoolean()) {
                    onSuccess.run();
                } else {
                    onIgnoredFailure.accept(new ElasticsearchException("simulated"));
                }
            } else {
                fail("replica [" + replica + "] was failed");
            }
        }
    }

    class TestReplicationOperation extends ReplicationOperation<Request, Request, TestPrimary.Result> {
        public TestReplicationOperation(Request request, Primary<Request, Request, TestPrimary.Result> primary,
                ActionListener<TestPrimary.Result> listener, Replicas<Request> replicas, Supplier<ClusterState> clusterStateSupplier) {
            this(request, primary, listener, true, false, replicas, clusterStateSupplier, ReplicationOperationTests.this.logger, "test");
        }

        public TestReplicationOperation(Request request, Primary<Request, Request, TestPrimary.Result> primary,
                ActionListener<TestPrimary.Result> listener, boolean executeOnReplicas, boolean checkWriteConsistency,
                Replicas<Request> replicas, Supplier<ClusterState> clusterStateSupplier, ESLogger logger, String opType) {
            super(request, primary, listener, executeOnReplicas, checkWriteConsistency, replicas, clusterStateSupplier, logger, opType);
        }
    }

    <T> void assertListenerThrows(String msg, PlainActionFuture<T> listener, Class<?> klass) throws InterruptedException {
        try {
            listener.get();
            fail(msg);
        } catch (ExecutionException ex) {
            assertThat(ex.getCause(), instanceOf(klass));
        }
    }

}
