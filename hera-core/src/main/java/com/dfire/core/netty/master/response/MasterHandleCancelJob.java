package com.dfire.core.netty.master.response;

import com.dfire.core.message.Protocol.*;
import com.dfire.core.netty.listener.MasterResponseListener;
import com.dfire.core.netty.listener.ResponseListener;
import com.dfire.core.netty.master.MasterContext;
import com.dfire.core.netty.util.AtomicIncrease;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author: <a href="mailto:lingxiao@2dfire.com">凌霄</a>
 * @time: Created in 下午3:42 2018/5/11
 * @desc master接收到worker端取消任务执行请求时，处理逻辑
 */
@Slf4j
public class MasterHandleCancelJob {

    public Future<Response> cancel(final MasterContext context, Channel channel, ExecuteKind kind, String jobId) {
        CancelMessage cancelMessage = CancelMessage.newBuilder()
                .setEk(kind)
                .setId(jobId)
                .build();
        final Request request = Request.newBuilder()
                .setRid(AtomicIncrease.getAndIncrement())
                .setOperate(Operate.Cancel)
                .setBody(cancelMessage.toByteString())
                .build();
        SocketMessage socketMessage = SocketMessage.newBuilder()
                .setKind(SocketMessage.Kind.REQUEST)
                .setBody(request.toByteString())
                .build();
        Future<Response> future = context.getThreadPool().submit(() -> {
            final CountDownLatch latch = new CountDownLatch(1);
            MasterResponseListener responseListener = new MasterResponseListener(request, context, false, latch, null);
            context.getHandler().addListener(responseListener);
            latch.await(3, TimeUnit.HOURS);
            if (!responseListener.getReceiveResult()) {
                log.error("取消任务信号消失，三小时未收到work返回：{}", jobId);
            }
            return responseListener.getResponse();
        });
        channel.writeAndFlush(socketMessage);
        return future;
    }
}
