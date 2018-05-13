package ru.spbau.mit.http;

import org.junit.Assert;
import org.junit.Test;
import ru.spbau.mit.tracker.request.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;

import static org.junit.Assert.*;

public class RequestTest {

    @Test
    public void parseTest() {
        final String list = "GET localhost?id=1 HTTP/1.1";
        final String sources = "GET localhost?id=3&fileID=1";
        final String upload = "GET localhost?id=2&name=hello123&size=100";
        final String update = "GET localhost?id=4&port=1234&size=5&value0=0&value1=1&value2=2&value3=3&value4=4";
        Assert.assertTrue(Request.parse(list) instanceof ListRequest);
        TrackerRequest request = Request.parse(sources);
        Assert.assertTrue(request instanceof SourcesRequest);
        Assert.assertEquals(1, ((SourcesRequest) request).getFileId());
        request = Request.parse(upload);
        Assert.assertTrue(request instanceof UploadRequest);
        Assert.assertEquals("hello123", ((UploadRequest) request).getName());
        Assert.assertEquals(100, ((UploadRequest) request).getSize());
        request = Request.parse(update);
        Assert.assertTrue(request instanceof UpdateRequest);
        Assert.assertEquals(1234, ((UpdateRequest) request).getClientDataInfo().getClientPort());
        ArrayList<Integer> l = new ArrayList<>();
        l.add(0);
        l.add(1);
        l.add(2);
        l.add(3);
        l.add(4);
        Assert.assertArrayEquals(l.toArray(), ((UpdateRequest) request).getClientDataInfo().getFilesId().toArray());
    }

    @Test
    public void getStringTest() {

    }
}