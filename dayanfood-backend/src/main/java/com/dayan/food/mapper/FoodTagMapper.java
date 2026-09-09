package com.dayan.food.mapper;
import com.dayan.food.entity.vo.FoodTagVO;
import org.apache.ibatis.annotations.Param;
import java.util.List;
public interface FoodTagMapper {
  List<FoodTagVO> findApproved(@Param("type") String type, @Param("keyword") String keyword);
  int insert(@Param("type") String type, @Param("name") String name, @Param("normalized") String normalized, @Param("userId") Long userId);
  FoodTagVO findById(@Param("id") Long id);
  FoodTagVO findByName(@Param("type") String type, @Param("normalized") String normalized);
}
