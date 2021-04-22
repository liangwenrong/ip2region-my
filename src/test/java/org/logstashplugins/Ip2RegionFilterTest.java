package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.FilterMatchListener;
import org.junit.Assert;
import org.junit.Test;
import org.logstash.plugins.ConfigurationImpl;
import org.logstash.plugins.ContextImpl;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.HashMap;

public class Ip2RegionFilterTest {

    @Test
    public void testJavaExampleFilter() throws FileNotFoundException {
        String sourceField = "ip";
        Map<String, Object> source = new HashMap<>();
        source.put("source", sourceField);
//        source.put("remove_field", new ArrayList<String >(){{
//            add("city");}});
        source.put("database", "E:\\coding\\IdeaProjects\\other\\ip2region\\data\\ip2region.db");
        Configuration config = new ConfigurationImpl(source);
        Context context = new ContextImpl(null, null);
        ip2region filter = new ip2region("test-id", config, context);

        Event e = new org.logstash.Event();
        TestMatchListener matchListener = new TestMatchListener();
        e.setField(sourceField, "111.231.236.91");
//        e.setField("add", );
        Collection<Event> results = filter.filter(Collections.singletonList(e), matchListener);

        Assert.assertEquals(1, results.size());
//        Assert.assertEquals("fedcba", e.getField(sourceField));
        Assert.assertEquals(1, matchListener.getMatchCount());
    }
}

class TestMatchListener implements FilterMatchListener {

    private AtomicInteger matchCount = new AtomicInteger(0);

    @Override
    public void filterMatched(Event event) {
        matchCount.incrementAndGet();
    }

    public int getMatchCount() {
        return matchCount.get();
    }
}