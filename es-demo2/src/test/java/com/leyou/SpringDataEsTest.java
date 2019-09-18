package com.leyou;

import com.alibaba.fastjson.JSON;
import com.leyou.pojo.Goods;
import org.apache.commons.beanutils.BeanUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.SearchResultMapper;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.aggregation.impl.AggregatedPageImpl;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class SpringDataEsTest {
    //这是elasticseach框架提供的
    private ElasticsearchTemplate elasticsearchTemplate;
    //根据springdataelasticsearch创造索引库和mapper文件
    @Test
    public void testAddIndex() {
        elasticsearchTemplate.createIndex(Goods.class);
    }

    @Test
    public void testAddMapping() {
        elasticsearchTemplate.putMapping(Goods.class);
    }
    private GoodsRepository goodsRepository;
  @Test
    public void testAddDoc() {
        Goods goods = new Goods("1", "小米9999手机", "手机", "小米", 1199.0, "q3311");
        goodsRepository.save(goods);
    }
   @Test
    public void testdeleteDoc() {
//        goodsRepository.deleteAll();   慎用
        goodsRepository.deleteById("1");
    }

    //批量新增
    @Test
    public void testAddBulkDoc() {
        ArrayList<Goods> list = new ArrayList<>();
        list.add(new Goods("1", "小米手机7", "手机", "小米", 3299.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Goods("2", "坚果手机R1", "手机", "锤子", 3699.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Goods("3", "华为META10", "手机", "华为", 4499.00,"http://image.leyou.com/13123.jpg"));
        list.add(new Goods("4", "小米Mix2S", "手机", "小米", 4299.00, "http://image.leyou.com/13123.jpg"));
        list.add(new Goods("5", "荣耀V10", "手机", "华为", 2799.00, "http://image.leyou.com/13123.jpg"));
        goodsRepository.saveAll(list);
    }

    //查询
    @Test
    public void testSearch() {
       /* List<Goods> goodList=goodsRepository.findByTitle("小米");
        List<Goods> goodsList=goodsRepository.findByBrand("小米");*/
//        List<Goods> goodList = goodsRepository.findByPriceBetween(2000.0, 5000.0);
        List<Goods> goodList = goodsRepository.findByBrandAndPriceBetween("小米", 2000.0, 5000.0);
        List<Goods> goodsList=goodsRepository.findByBrandOrPriceBetween("小米", 2000.0, 5000.0);
        goodList.forEach(goods -> {
            System.out.println(goods);
        });
    }

    //SDE结合原生查询
    public void testQuery() {
        NativeSearchQueryBuilder nativeSearchQueryBuilder = new NativeSearchQueryBuilder();
        nativeSearchQueryBuilder.withQuery(QueryBuilders.termQuery("title", "小米"));
     /*   nativeSearchQueryBuilder.withQuery(QueryBuilders.matchAllQuery());*/
        //构建高亮的条件
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span stype='color:red'>");
        highlightBuilder.postTags("</span>");
        highlightBuilder.field("title");
        nativeSearchQueryBuilder.withHighlightBuilder(highlightBuilder);
        nativeSearchQueryBuilder.withHighlightFields(new HighlightBuilder.Field("title"));
        elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class, new SearchResultMapperImpl());

        //分页
        nativeSearchQueryBuilder.withPageable(PageRequest.of(0, 2));
        //构建聚合的条件
        nativeSearchQueryBuilder.addAggregation(AggregationBuilders.terms("brandCount").field("brand"));
        //返回的跟聚合有关的
        AggregatedPage<Goods> goodsAggregatedPage = elasticsearchTemplate.queryForPage(nativeSearchQueryBuilder.build(), Goods.class);
        List<Goods> content = goodsAggregatedPage.getContent();
        content.forEach(goods -> {
            System.out.println(goods);
        });
        //获取聚合的结果
        Terms terms = goodsAggregatedPage.getAggregations().get("brandCount");
        List<? extends Terms.Bucket> buckets = terms.getBuckets();
        buckets.forEach(bucket->{
            System.out.println(bucket.getKeyAsString()+":"+bucket.getDocCount());
        });
      /*  //获取实体类中的所有属性值
        List<Goods> goodsList = goodsAggregatedPage.getContent();
        goodsList.forEach(goods -> {
            System.out.println(goods);

        });*/
    }

    private class SearchResultMapperImpl implements  SearchResultMapper{
        @Override
        public <T> AggregatedPage<T> mapResults(SearchResponse response, Class<T> clazz, Pageable pageable) {
            long total = response.getHits().getTotalHits();//返回时需要的参数
            Aggregations aggregations = response.getAggregations();//返回时需要的参数
            String scrollId = response.getScrollId();//返回时需要的参数
            float maxScore = response.getHits().getMaxScore();//返回时需要的参数
            //处理我们想要高亮的结果
            SearchHit[] hits = response.getHits().getHits();
            List<T> content = new ArrayList<>();
            for (SearchHit hit : hits) {
                String jsonString = hit.getSourceAsString();
                T t = JSON.parseObject(jsonString, clazz);
                //
                Map<String, HighlightField> highlightFields = hit.getHighlightFields();
                HighlightField highlightField = highlightFields.get("title");
                Text[] fragments = highlightField.getFragments();
                if (fragments != null && fragments.length> 0) {
                    String title = fragments[0].toString();
                    try {
                        BeanUtils.copyProperty(t,"title",title);
                    } catch (Exception e) {
                        /*e.printStackTrace();*/
                        System.out.println("ssss");
                    }
                }
                content.add(t);
            }
             //new 的是实现类因为是抽象的
            return new AggregatedPageImpl<>(content, pageable,  total,  aggregations,  scrollId, maxScore);
        }
    }
}
