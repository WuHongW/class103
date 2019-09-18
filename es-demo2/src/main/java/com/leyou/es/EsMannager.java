package com.leyou.es;

import com.alibaba.fastjson.JSON;
import com.google.gson.Gson;
import com.leyou.pojo.Item;
import org.apache.http.HttpHost;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//客户端
public class EsMannager {
    RestHighLevelClient client = null;

    Gson gson = new Gson();

    @Before
    public void init() throws Exception {
        client = new RestHighLevelClient(
                RestClient.builder(
                        new HttpHost("127.0.0.1", 9201, "http"),
                        new HttpHost("127.0.0.1", 9202, "http"),
                        new HttpHost("127.0.0.1", 9203, "http")));
    }

    @Test
    //新增和修改
    public void testDoc() throws Exception {

        //封装对象需要转jsion
        Item item = new Item("1", "小米9手机", "手机", "小米", 1199.0, "q3311");
        //插入索引数据
        IndexRequest request = new IndexRequest("item", "docs", item.getId());
//        String toJSON = (String) JSON.toJSON(item);
        String toJson = gson.toJson(item);
        request.source(toJson, XContentType.JSON);
        //放入数据库
        client.index(request, RequestOptions.DEFAULT);

    }

    //删除
    @Test
    public void testdeleteDoc() throws Exception {
        DeleteRequest deleteRequest = new DeleteRequest("item", "docs", "1");
        client.delete(deleteRequest, RequestOptions.DEFAULT);
    }

    @Test
    //批量新增
    public void testBulkAddDoc() throws Exception {
        ArrayList<Item> list = new ArrayList<>();
        list.add(new Item("1", "小米手机7", "手机", "小米", 3299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item("2", "坚果手机R1", "手机", "锤子", 3699.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item("3", "华为META10", "手机", "华为", 4499.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item("4", "小米Mix2S", "手机", "小米", 4299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Item("5", "荣耀V10", "手机", "华为", 2799.00, "http://image.leyou.com/13123.jpg"));
        BulkRequest bulkRequest = new BulkRequest();
        for (Item item : list) {
            //插入索引数据
            IndexRequest indexrequest = new IndexRequest("item", "docs", item.getId());
//           String toJSON = (String) JSON.toJSON(item);
            String toJson = gson.toJson(item);
            indexrequest.source(toJson, XContentType.JSON);
            bulkRequest.add(indexrequest);
        }
        client.bulk(bulkRequest, RequestOptions.DEFAULT);
    }

    @Test
    public void testSearch() throws Exception {
        SearchRequest searchRequest = new SearchRequest("item").types("docs");

        //构建查询方式
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //查询所有
        searchSourceBuilder.query(QueryBuilders.matchAllQuery());
        //term查询
//        searchSourceBuilder.query(QueryBuilders.termQuery("title", "小米"));
//        searchSourceBuilder.fetchSource(new String[]{"id", "title"}, null); //过滤
/*
        searchSourceBuilder.fetchSource(null,new String[]{"id", "title"}); //过滤
*/
       /* searchSourceBuilder.query(QueryBuilders.termQuery("title", "手机")); //过滤
        searchSourceBuilder.postFilter(QueryBuilders.termQuery("brand", "锤子"));*/
        searchSourceBuilder.from(0);
        searchSourceBuilder.from(20);
        searchSourceBuilder.sort("price", SortOrder.DESC);
        searchSourceBuilder.query(QueryBuilders.termQuery("title", "小米"));
       /* //构建高亮的条件
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("title");
        highlightBuilder.preTags("<span style='color:red'");
        highlightBuilder.postTags("</span>");
        searchSourceBuilder.highlighter(highlightBuilder);*/
       //聚合
        searchSourceBuilder.aggregation(AggregationBuilders.terms("brandCount").field("brand"));

//        searchSourceBuilder.query(QueryBuilders.matchQuery());
//        searchSourceBuilder.query(QueryBuilders.wildcardQuery());
//        searchSourceBuilder.query(QueryBuilders.fuzzyQuery());
        //放到searchRequest中
        searchRequest.source(searchSourceBuilder);
        //执行查询
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        //获取聚合的结果
        Aggregations aggregations = searchResponse.getAggregations();
        Terms terms = aggregations.get("brandCount");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        buckets.forEach(bucket->{
            System.out.println(bucket.getKeyAsString() + ":" + bucket.getDocCount());
        });
        SearchHits responseHits = searchResponse.getHits();
        System.out.println("总记录数:" + responseHits.getHits());
        SearchHit[] searchHits = responseHits.getHits();
        for (SearchHit searchHit : searchHits) {
            //把json字符串转成对象
            String jsonString = searchHit.getSourceAsString();
            //*把值封装进对象里了
            System.out.println(jsonString);
            //fastjson转
            Item item = JSON.parseObject(jsonString, Item.class);
            System.out.println(item);
            //获取高亮的结果
            Map<String, HighlightField> highlightFields = searchHit.getHighlightFields();
            HighlightField highlightField = highlightFields.get("title");
            Text[] fragments = highlightField.getFragments();
            if (fragments != null && fragments.length > 0) {
                String tile = fragments[0].toString();
                item.setTitle(tile);

            }
        }
    }

    @After
    public void end() throws Exception {
        client.close();
    }
}
