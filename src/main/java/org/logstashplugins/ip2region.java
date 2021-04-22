package org.logstashplugins;

import co.elastic.logstash.api.Configuration;
import co.elastic.logstash.api.Context;
import co.elastic.logstash.api.Event;
import co.elastic.logstash.api.Filter;
import co.elastic.logstash.api.FilterMatchListener;
import co.elastic.logstash.api.LogstashPlugin;
import co.elastic.logstash.api.PluginConfigSpec;
import ip2region.DataBlock;
import ip2region.DbConfig;
import ip2region.DbMakerConfigException;
import ip2region.DbSearcher;

import java.io.FileNotFoundException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

// class name must match plugin name
@LogstashPlugin(name = "ip2region")
public class ip2region implements Filter {

    public static final PluginConfigSpec<String> SOURCE_CONFIG =
            PluginConfigSpec.stringSetting("source", "ip");//default is "ip"

    public static final PluginConfigSpec<String> DB_FILE_CONFIG =
            PluginConfigSpec.stringSetting("database", null);

    public static final PluginConfigSpec<List<Object>> ADD_FIELD_CONFIG =
            PluginConfigSpec.arraySetting("add_field", new ArrayList() {{
                add("city_id");
                add("region");
                add("data_ptr");

                add("country");
                add("province");
                add("city");
                add("net_type");
            }}, false, false);

    public static final PluginConfigSpec<List<Object>> REMOVE_FIELD_CONFIG =
            PluginConfigSpec.arraySetting("remove_field");


    private String id;
    private String ipField;

    private List<Object> addFields;
    private List<Object> removeFields;

    private String dbfile = "";


    public ip2region(String id, Configuration config, Context context) throws FileNotFoundException {
        // constructors should validate configuration options
        this.id = id;
        this.ipField = config.get(SOURCE_CONFIG);

        this.addFields = config.get(ADD_FIELD_CONFIG);
        this.removeFields = config.get(REMOVE_FIELD_CONFIG);
        if (removeFields != null) {
            Iterator<Object> iterator = removeFields.iterator();
            while (iterator.hasNext()) {
                Object removeField = iterator.next();
                if (addFields.contains(removeField)) {
                    addFields.remove(removeField);
                    iterator.remove();
                }
            }
        }

        dbfile = config.get(DB_FILE_CONFIG);
        if (dbfile == null) {
            throw new FileNotFoundException("database => \"/path/to/dbfile\" not set");
        }
        config(dbfile, DbSearcher.BTREE_ALGORITHM);
    }

    @Override
    public Collection<Event> filter(Collection<Event> events, FilterMatchListener matchListener) {
        for (Event e : events) {
            Object f = e.getField(ipField);
            if (f instanceof String) {
                DataBlock ip_msg = getRegion((String) f);
                if (addFields.contains("city_id")) {
                    e.setField("city_id", ip_msg.getCityId());
                }
//                region -> {RubyString@8500} "中国|0|广东|广州|电信"
                String region = ip_msg.getRegion();
                if (addFields.contains("region")) {
                    e.setField("region", region);
                }
                if (addFields.contains("data_ptr")) {
                    e.setField("data_ptr", ip_msg.getDataPtr());
                }
                if (region != null) {
                    String[] split = region.split("\\|");
                    if (split.length > 4) {
                        if (addFields.contains("country")) {
                            e.setField("country", split[0]);
                        }
                        if (addFields.contains("province")) {
                            e.setField("province", split[2]);
                        }
                        if (addFields.contains("city")) {
                            e.setField("city", split[3]);
                        }
                        if (addFields.contains("net_type")) {
                            e.setField("net_type", split[4]);
                        }
                    }
                }
            }
            if (removeFields != null && removeFields.size() > 0) {
                for (Object removeField : removeFields) {
                    if (removeField instanceof String) {
                        e.remove((String) removeField);
                    }
                }
            }
            matchListener.filterMatched(e);
        }
        return events;
    }

    @Override
    public Collection<PluginConfigSpec<?>> configSchema() {
        // should return a list of all configuration options for this plugin
        return Collections.singletonList(SOURCE_CONFIG);
    }

    @Override
    public String getId() {
        return this.id;
    }


    public DataBlock getRegion(String ip) {
        if (ip == null) {
            return new DataBlock(0, "");
        }
        ip = ip.trim();
        try {
            return (DataBlock) method.invoke(searcher, ip);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.out.println("method.invoke(searcher, ip) error");
            return new DataBlock(-1, ip);
        }
    }

    public DbSearcher searcher;
    public Method method;

    public void config(String dbFilePath, int algorithm) throws FileNotFoundException {

        /*File dbFile = new File(dbFilePath);
        if (dbFile.exists() == false) {
            System.out.println("Error: Invalid ip2region.db file path:" + dbFilePath);
            throw new FileNotFoundException("Error: Invalid ip2region.db file path:" + dbFilePath);
        }*/
        if (algorithm < 1 || algorithm > 3) {
            algorithm = DbSearcher.BTREE_ALGORITHM;
        }
        try {
            DbConfig config = new DbConfig();
            searcher = new DbSearcher(config, dbFilePath);

            switch (algorithm) {
                case DbSearcher.BTREE_ALGORITHM:
                    method = searcher.getClass().getMethod("btreeSearch", String.class);
                    break;
                case DbSearcher.BINARY_ALGORITHM:
                    method = searcher.getClass().getMethod("binarySearch", String.class);
                    break;
                case DbSearcher.MEMORY_ALGORITYM:
                    method = searcher.getClass().getMethod("memorySearch", String.class);
                    break;
                default:
                    System.out.println("algorithm=" + algorithm + " is not right");
                    throw new RuntimeException("algorithm=" + algorithm + " is not right");
            }
        } catch (DbMakerConfigException | NoSuchMethodException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
