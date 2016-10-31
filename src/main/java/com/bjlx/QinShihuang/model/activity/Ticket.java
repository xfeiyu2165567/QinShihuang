package com.bjlx.QinShihuang.model.activity;

import org.bson.types.ObjectId;
import org.hibernate.validator.constraints.NotBlank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

/**
 * Created by pengyt on 2016/7/22.
 * 门票
 */
@Entity
public class Ticket {

    public final static String fd_id = "id";
    public final static String fd_price = "price";
    public final static String fd_marketPrice = "marketPrice";
    public final static String fd_free = "free";
    public final static String fd_refundWay = "refundWay";
    public final static String fd_refundDesc = "refundDesc";
    public final static String fd_desc = "desc";
    public final static String fd_maxNum = "maxNum";

    /**
     * 主键
     */
    @NotBlank
    @Id
    private ObjectId id;

    /**
     * 门票价格
     */
    private Double price;

    /**
     * 原价
     */
    private Double marketPrice;

    /**
     * 是否免费
     */
    private Boolean free;

    /**
     * 退款方式，1表示退款到平台公共账号，2表示原路返回，3表示不接受退款
     */
    private Integer refundWay = 2;

    /**
     * 委托平台说明
     */
    private String refundDesc;

    /**
     * 票种说明
     */
    private String desc;

    /**
     * 最大数量
     */
    private Integer maxNum;

    public ObjectId getId() {
        return id;
    }

    public void setId(ObjectId id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean isFree() {
        return free;
    }

    public void setFree(Boolean free) {
        this.free = free;
    }

    public Integer getRefundWay() {
        return refundWay;
    }

    public void setRefundWay(Integer refundWay) {
        this.refundWay = refundWay;
    }

    public String getRefundDesc() {
        return refundDesc;
    }

    public void setRefundDesc(String refundDesc) {
        this.refundDesc = refundDesc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public Integer getMaxNum() {
        return maxNum;
    }

    public void setMaxNum(Integer maxNum) {
        this.maxNum = maxNum;
    }

    public Double getMarketPrice() {
        return marketPrice;
    }

    public void setMarketPrice(Double marketPrice) {
        this.marketPrice = marketPrice;
    }

    public Ticket(Boolean free, Integer maxNum) {
        this.id = new ObjectId();
        this.free = free;
        this.maxNum = maxNum;
    }
}
