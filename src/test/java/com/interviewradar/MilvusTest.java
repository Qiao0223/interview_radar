package com.interviewradar;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import org.junit.Test;

import static org.junit.Assert.*;

public class MilvusTest {

    @Test
    public void testConnectionSuccessful() {
        MilvusServiceClient client = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost("115.190.83.184")
                        .withPort(19530)
                        .build()
        );

        R<?> response = client.getVersion();
        assertEquals((long) R.Status.Success.getCode(), response.getStatus().longValue());
    }
}
