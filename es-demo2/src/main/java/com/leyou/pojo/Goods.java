package com.leyou.pojo;

import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
//springDataElasticSearch持久层操作索引库和映射
@Document(indexName = "leyou", type = "goods", shards = 3, replicas = 1)
public class Goods {
    @Field(type = FieldType.Keyword)
    private String id;     //不分词
    @Field(type = FieldType.Text,analyzer = "ik_max_word",index = true,store = true)
    private String title; //标题   分词
    @Field(type = FieldType.Keyword,index = true,store = true)
    private String category;// 分类   不分词
    @Field(type = FieldType.Keyword,index = true,store = true)
    private String brand; // 品牌     不分词
    @Field(type = FieldType.Keyword,index = true,store = true)
    private Double price; // 价格    不分词
    @Field(type = FieldType.Keyword,index = true,store = true)
    private String images; // 图片地址   不分词

    public Goods() {
    }

    public Goods(String id, String title, String category, String brand, Double price, String images) {
        this.id = id;
        this.title = title;
        this.category = category;
        this.brand = brand;
        this.price = price;
        this.images = images;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public String getImages() {
        return images;
    }

    public void setImages(String images) {
        this.images = images;
    }

    @Override
    public String toString() {
        return "Item{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", brand='" + brand + '\'' +
                ", price=" + price +
                ", images='" + images + '\'' +
                '}';
    }
}
