package com.ctrip.xpipe.redis.meta.server.keeper;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.ctrip.xpipe.tuple.Pair;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.runners.MockitoJUnitRunner;

import com.ctrip.xpipe.lifecycle.LifecycleHelper;
import com.ctrip.xpipe.redis.core.entity.KeeperMeta;
import com.ctrip.xpipe.redis.meta.server.AbstractMetaServerTest;
import com.ctrip.xpipe.redis.meta.server.meta.CurrentMetaManager;
import com.ctrip.xpipe.redis.meta.server.meta.DcMetaCache;


/**
 * @author wenchao.meng
 *
 * Jan 4, 2017
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultKeeperStateChangeHandlerTest extends AbstractMetaServerTest{
	
	private DefaultKeeperStateChangeHandler handler;
	
	@Mock
	private CurrentMetaManager currentMetaManager;
	
	@Mock
	private DcMetaCache dcMetaCache;
	
	private String clusterId, shardId;
	
	private List<KeeperMeta> keepers;
	
	private Pair<String, Integer> keeperMaster;
	
	private int setStateTimeMilli = 1500;
	
	private AtomicInteger calledCount = new AtomicInteger();

	@Before
	public void beforeDefaultKeeperStateChangeHandlerTest() throws Exception{
		
		handler = new DefaultKeeperStateChangeHandler();
		handler.setClientPool(getXpipeNettyClientKeyedObjectPool());
		handler.setcurrentMetaManager(currentMetaManager);
		handler.setDcMetaCache(dcMetaCache);
		handler.setScheduled(scheduled);
		handler.setExecutors(executors);
		
		clusterId = getClusterId();
		shardId = getShardId();
		
		keepers = createRandomKeepers(2);
		
		keeperMaster = new Pair<>("localhost", randomPort());
		
		LifecycleHelper.initializeIfPossible(handler);
		LifecycleHelper.startIfPossible(handler);
		add(handler);

		startServer(keepers.get(0).getPort(), new Callable<String>() {

			@Override
			public String call() throws Exception {
				calledCount.incrementAndGet();
				sleep(setStateTimeMilli);
				return "+OK\r\n";
			}
		});

		startServer(keepers.get(1).getPort(), new Callable<String>() {

			@Override
			public String call() throws Exception {
				return "+OK\r\n";
			}
		});

		when(currentMetaManager.getSurviveKeepers(clusterId, shardId)).thenReturn(keepers);
		when(currentMetaManager.getKeeperMaster(clusterId, shardId)).thenReturn(keeperMaster);
		when(dcMetaCache.isCurrentDcPrimary(clusterId, shardId)).thenReturn(true);
	}
	
	@Test
	public void testDifferentShardExecute() throws Exception{

		String clusterId1 = clusterId + "1";
		String shardId1 = shardId + "1";
		
		when(currentMetaManager.getSurviveKeepers(clusterId1, shardId1)).thenReturn(keepers);
		when(currentMetaManager.getKeeperMaster(clusterId1, shardId1)).thenReturn(keeperMaster);
		when(dcMetaCache.isCurrentDcPrimary(clusterId1, shardId1)).thenReturn(true);


		handler.keeperActiveElected(clusterId, shardId, null);
		handler.keeperActiveElected(clusterId1, shardId1, null);

		sleep(setStateTimeMilli/2);
		Assert.assertEquals(2, calledCount.get());
	}
	
	@Test
	public void testSameShardExecute() throws Exception{
		
		handler.keeperActiveElected(clusterId, shardId, null);
		handler.keeperActiveElected(clusterId, shardId, null);
		sleep(setStateTimeMilli/2);
		Assert.assertEquals(1, calledCount.get());
		sleep(setStateTimeMilli*3/2);
		Assert.assertEquals(2, calledCount.get());
	}

	@After
	public void adterDefaultKeeperStateChangeHandlerTest(){
		
	}
}
