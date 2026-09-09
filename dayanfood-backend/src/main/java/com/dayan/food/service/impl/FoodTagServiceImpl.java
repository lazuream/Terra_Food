package com.dayan.food.service.impl;
import com.dayan.food.entity.dto.FoodTagCreateDTO;
import com.dayan.food.entity.vo.FoodTagVO;
import com.dayan.food.mapper.AppUserMapper;
import com.dayan.food.mapper.FoodTagMapper;
import com.dayan.food.service.FoodTagService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import java.util.List;
@Service public class FoodTagServiceImpl implements FoodTagService {
 private final FoodTagMapper mapper; private final AppUserMapper users;
 public FoodTagServiceImpl(FoodTagMapper mapper, AppUserMapper users){this.mapper=mapper;this.users=users;}
 public List<FoodTagVO> list(String type,String keyword){return mapper.findApproved(type,keyword==null?null:keyword.trim());}
 public FoodTagVO create(FoodTagCreateDTO request,String username){
  if(!List.of("TASTE","INGREDIENT","CUISINE").contains(request.type())) throw new IllegalArgumentException("标签类型不合法");
  var user=users.findByUsername(username); if(user==null) throw new IllegalArgumentException("用户不存在");
  String name=request.name().trim(); try{mapper.insert(request.type(),name,name.toLowerCase(),user.getId());}catch(DuplicateKeyException e){throw new IllegalArgumentException("标签已存在");}
  return mapper.findByName(request.type(),name.toLowerCase());
 }
}
