package com.generated.qualityTrace.repositories;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.generated.qualityTrace.models.ProductBatch;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface ProductBatchMapper extends BaseMapper<ProductBatch> {

  @Select("SELECT * FROM product_batch WHERE batch_no = #{batchNo} LIMIT 1")
  ProductBatch findByBatchNo(@Param("batchNo") String batchNo);
}
