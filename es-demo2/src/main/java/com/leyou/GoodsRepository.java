package com.leyou;

import com.leyou.pojo.Goods;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

//springdatasearch操作文档自定义的接口,然后再去搞测试类
public interface GoodsRepository extends ElasticsearchRepository<Goods,String> {
    List<Goods> findByTitle(String 小米);

    List<Goods> findByBrand(String 小米);

    List<Goods> findByPriceBetween(double v, double v1);

    List<Goods> findByBrandAndPriceBetween(String 小米, double v, double v1);

    List<Goods> findByBrandOrPriceBetween(String 小米, double v, double v1);
}
